/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.exceptions.AssuranceIncompleteScanDefinitionException;
import com.markallenjohnson.assurance.exceptions.AssuranceNullFileReferenceException;
import com.markallenjohnson.assurance.model.IModelDelegate;
import com.markallenjohnson.assurance.model.compare.IScanOptions;
import com.markallenjohnson.assurance.model.concurrency.AssuranceThreadPool;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

/**
 * Synchronous, transactional facade exposing the engine's read/write
 * operations to the JSON-RPC layer.
 *
 * <p>The Swing UI invokes the engine asynchronously via {@code SwingWorker} +
 * the {@link com.markallenjohnson.assurance.ApplicationDelegate} event bus
 * because Swing's threading model demands it. The IPC layer has the opposite
 * shape: every JSON-RPC call is a request/response pair that must complete on
 * the dispatcher thread. This bean wraps {@link IModelDelegate} so handlers
 * can call into the model with a Spring-managed transaction (and
 * Hibernate-managed persistence context) open for the duration of the call,
 * without rebuilding the Spring context per request the way the SwingWorkers
 * do.
 *
 * <p>Methods on this facade intentionally remain narrow: each one corresponds
 * to a single JSON-RPC method documented in {@code docs/ipc-contract.md}.
 *
 * <p>Asynchronous operations (currently {@code performScan}, with merge and
 * restore to follow) start work on a dedicated executor and emit progress
 * via {@link IpcNotifier} server-initiated notifications. The
 * {@link ScanRunRegistry} enforces single-scan-per-definition concurrency.
 */
@Component("EngineFacade")
public class EngineFacade
{
	private static final Logger logger = LogManager.getLogger(EngineFacade.class);

	private final IModelDelegate modelDelegate;
	private final Supplier<IAssuranceThreadPool> scanThreadPoolFactory;
	private final ScanRunRegistry scanRunRegistry;
	private final ConcurrentRunRegistry resultMergeRegistry;
	private final ConcurrentRunRegistry wholeScanMergeRegistry;
	private final ConcurrentRunRegistry restoreRegistry;
	private final ExecutorService scanExecutor;
	private final ExecutorService mutationExecutor;

	private volatile IpcNotifier notifier;

	@Autowired
	public EngineFacade(IModelDelegate modelDelegate)
	{
		this(modelDelegate, AssuranceThreadPool::new, new ScanRunRegistry(),
			new ConcurrentRunRegistry(), new ConcurrentRunRegistry(), new ConcurrentRunRegistry(),
			Executors.newSingleThreadExecutor(EngineFacade::namedScanThread),
			Executors.newSingleThreadExecutor(EngineFacade::namedMutationThread));
	}

	EngineFacade(IModelDelegate modelDelegate, Supplier<IAssuranceThreadPool> scanThreadPoolFactory,
		ScanRunRegistry scanRunRegistry, ConcurrentRunRegistry resultMergeRegistry,
		ConcurrentRunRegistry wholeScanMergeRegistry, ConcurrentRunRegistry restoreRegistry,
		ExecutorService scanExecutor, ExecutorService mutationExecutor)
	{
		this.modelDelegate = modelDelegate;
		this.scanThreadPoolFactory = scanThreadPoolFactory;
		this.scanRunRegistry = scanRunRegistry;
		this.resultMergeRegistry = resultMergeRegistry;
		this.wholeScanMergeRegistry = wholeScanMergeRegistry;
		this.restoreRegistry = restoreRegistry;
		this.scanExecutor = scanExecutor;
		this.mutationExecutor = mutationExecutor;
	}

	/**
	 * Wires the IPC notification publisher. The {@link WebSocketIpcServer}
	 * cannot be constructor-injected because it depends on this facade, so
	 * the wiring is set after both have been constructed (in
	 * {@code Application.runHeadless}).
	 */
	public void setNotifier(IpcNotifier notifier)
	{
		this.notifier = notifier;
	}

	public ScanRunRegistry getScanRunRegistry()
	{
		return this.scanRunRegistry;
	}

	/**
	 * Returns all persisted scan definitions. The transactional boundary is
	 * inherited by the underlying {@code IModelDelegate.getScanDefinitions()}
	 * call; we mark this method {@code readOnly} as well so the JPA provider
	 * can opt into read-optimized handling.
	 *
	 * <p>{@link LoadScanDefinitionsHandler} serializes each definition after this
	 * method returns (outside the persistence context). Child graphs must therefore
	 * be fully initialised here — same pattern as {@link #loadScans()} — or JSON
	 * projection raises {@code LazyInitializationException}.
	 */
	@Transactional(readOnly = true)
	public List<ScanDefinition> loadScanDefinitions()
	{
		List<ScanDefinition> definitions = this.modelDelegate.getScanDefinitions();
		if (definitions != null)
		{
			for (ScanDefinition def : definitions)
			{
				Hibernate.initialize(def.getUnmodifiableScanMapping());
				for (ScanMappingDefinition mapping : def.getUnmodifiableScanMapping())
				{
					mapping.getSource();
					mapping.getTarget();
					Hibernate.initialize(mapping.getUnmodifiableExclusions());
					for (FileReference exclusion : mapping.getUnmodifiableExclusions())
					{
						if (exclusion != null)
						{
							initialiseFileReference(exclusion);
						}
					}
				}
			}
		}
		return definitions;
	}

	/**
	 * Returns the persisted scan history. Each {@link Scan} is returned with
	 * its {@code results} collection forced to be initialised (under the open
	 * read-only transaction) so the IPC layer can render the
	 * {@code resultCount} field without a {@code LazyInitializationException}
	 * after the persistence context closes. Equivalent in shape to the legacy
	 * {@code IApplicationDelegate.getScans()} surface backing the Swing
	 * {@code HistoryDialog}.
	 */
	@Transactional(readOnly = true)
	public List<Scan> loadScans()
	{
		List<Scan> scans = this.modelDelegate.getScans();
		if (scans != null)
		{
			for (Scan scan : scans)
			{
				// Force initialization while the session is open; ScanJson
				// reads getUnmodifiableResults().size() to project resultCount.
				Hibernate.initialize(scan.getUnmodifiableResults());
			}
		}
		return scans;
	}

	/**
	 * Returns the comparison results for a given scan id, or {@code null}
	 * when no scan exists with that id (the handler maps {@code null} to
	 * JSON-RPC {@code -32002}). Each result's source / target
	 * {@link FileReference} and inline {@link com.markallenjohnson.assurance.model.entities.FileAttributes}
	 * graphs are eagerly initialised under the open transaction so the IPC
	 * layer can render the projection after the persistence context closes
	 * without raising {@code LazyInitializationException}.
	 */
	@Transactional(readOnly = true)
	public List<ComparisonResult> loadScanResults(Long scanId)
	{
		if (scanId == null)
		{
			return null;
		}
		Scan scan = this.modelDelegate.getScanById(scanId);
		if (scan == null)
		{
			return null;
		}
		List<ComparisonResult> results = this.modelDelegate.getScanResults(scan);
		if (results != null)
		{
			for (ComparisonResult result : results)
			{
				initialiseFileReference(result.getSource());
				initialiseFileReference(result.getTarget());
			}
		}
		return results;
	}

	/**
	 * Loads a single comparison result by primary key, or {@code null} when no
	 * row exists. Lazily initialised file-reference graphs are traversed under
	 * the same read-only transaction semantics as
	 * {@link #loadScanResults(Long)} so the IPC codec can project the entity
	 * without {@link org.hibernate.LazyInitializationException}.
	 */
	@Transactional(readOnly = true)
	public ComparisonResult loadComparisonResult(Long resultId)
	{
		if (resultId == null)
		{
			return null;
		}
		ComparisonResult result = this.modelDelegate.getResultById(resultId);
		if (result == null)
		{
			return null;
		}
		this.initialiseFileReference(result.getSource());
		this.initialiseFileReference(result.getTarget());
		return result;
	}

	private void initialiseFileReference(FileReference reference)
	{
		if (reference == null)
		{
			return;
		}
		Hibernate.initialize(reference);
		if (reference.getFileAttributes() != null)
		{
			Hibernate.initialize(reference.getFileAttributes());
		}
	}

	/**
	 * Persists a scan definition (create or update) and returns the managed
	 * instance with its database-assigned id populated. The caller is
	 * responsible for shaping the {@link ScanDefinition} graph from the wire
	 * payload (including its {@code scanMapping} children); this method just
	 * delegates the persist + flush to the model layer.
	 */
	@Transactional
	public ScanDefinition saveScanDefinition(ScanDefinition scanDefinition)
	{
		return this.modelDelegate.saveScanDefinition(scanDefinition);
	}

	/**
	 * Deletes a scan definition by id. Returns {@code true} if the entity
	 * existed and was removed, {@code false} if no entity with that id was
	 * found. The handler maps {@code false} to JSON-RPC {@code -32002}
	 * (entity not found) per {@code docs/ipc-contract.md}; runtime failures
	 * propagate as {@link RuntimeException} for the handler to translate to
	 * {@code -32001} (engine error).
	 */
	@Transactional
	public boolean deleteScanDefinitionById(Long id)
	{
		ScanDefinition existing = this.modelDelegate.getScanDefinitionById(id);
		if (existing == null)
		{
			return false;
		}
		this.modelDelegate.deleteScanDefinition(existing);
		return true;
	}

	/**
	 * Deletes a scan (and its results) by id. Returns {@code true} if the
	 * entity existed and was removed, {@code false} if no entity with that id
	 * was found. The handler maps {@code false} to JSON-RPC {@code -32002}
	 * (entity not found) per {@code docs/ipc-contract.md}; runtime failures
	 * propagate as {@link RuntimeException} for the handler to translate to
	 * {@code -32001} (engine error).
	 */
	@Transactional
	public boolean deleteScanById(Long id)
	{
		Scan existing = this.modelDelegate.getScanById(id);
		if (existing == null)
		{
			return false;
		}
		this.modelDelegate.deleteScan(existing);
		return true;
	}

	/**
	 * Returns the persisted {@link ApplicationConfiguration} (creating the
	 * default singleton row if none exists, mirroring the legacy 1.x semantics
	 * in {@link com.markallenjohnson.assurance.model.ModelDelegate#getApplicationConfiguration}).
	 * The {@link ApplicationConfiguration#initialize()} call inside the model
	 * delegate populates the parsed list views ({@code ignoredFileNamesCollection}
	 * / {@code ignoredFileExtensionsCollection}) the IPC layer projects onto
	 * the wire.
	 */
	@Transactional
	public ApplicationConfiguration loadApplicationConfiguration()
	{
		return this.modelDelegate.getApplicationConfiguration();
	}

	/**
	 * Persists an application configuration (create or update) and returns the
	 * managed instance. The model delegate re-derives the parsed list views
	 * after flush so the returned wire payload matches what a follow-up
	 * {@link #loadApplicationConfiguration()} would produce.
	 */
	@Transactional
	public ApplicationConfiguration saveApplicationConfiguration(ApplicationConfiguration config)
	{
		return this.modelDelegate.saveApplicationConfiguration(config);
	}

	/**
	 * Result status of {@link #startScan(Long, boolean) startScan}. The
	 * handler maps {@link #STARTED} to a success response carrying the new
	 * {@code scanId}, {@link #DEFINITION_NOT_FOUND} to JSON-RPC
	 * {@code -32002}, and {@link #ALREADY_RUNNING} to JSON-RPC
	 * {@code -32003}.
	 */
	public enum StartScanStatus
	{
		STARTED,
		DEFINITION_NOT_FOUND,
		ALREADY_RUNNING,
	}

	public static final class StartScanOutcome
	{
		private final StartScanStatus status;
		private final Long scanId;

		private StartScanOutcome(StartScanStatus status, Long scanId)
		{
			this.status = status;
			this.scanId = scanId;
		}

		public StartScanStatus getStatus()
		{
			return status;
		}

		public Long getScanId()
		{
			return scanId;
		}
	}

	/**
	 * Synchronously creates a {@link Scan} record (so its id can be returned
	 * immediately) and submits the comparison work for asynchronous
	 * execution. Progress is reported via {@link IpcNotifier} notifications;
	 * see {@code docs/ipc-contract.md} for the wire shape.
	 *
	 * @param scanDefinitionId id of the scan definition to run
	 * @param merge {@code true} to also merge the scan results after
	 *   comparison completes (mirrors the legacy
	 *   {@code ApplicationDelegate.performScan(scanDefinition, merge)} flag)
	 * @return outcome carrying the new {@code scanId} on success, or a
	 *   non-{@code STARTED} status when the request was rejected
	 */
	public StartScanOutcome startScan(Long scanDefinitionId, boolean merge)
	{
		Long scanId = openScanShell(scanDefinitionId);
		if (scanId == null)
		{
			return new StartScanOutcome(StartScanStatus.DEFINITION_NOT_FOUND, null);
		}
		if (!this.scanRunRegistry.tryAcquire(scanDefinitionId, scanId))
		{
			Long otherId = this.scanRunRegistry.currentScanId(scanDefinitionId);
			return new StartScanOutcome(StartScanStatus.ALREADY_RUNNING, otherId);
		}

		this.scanExecutor.submit(() -> runScan(scanDefinitionId, scanId, merge));
		return new StartScanOutcome(StartScanStatus.STARTED, scanId);
	}

	@Transactional
	protected Long openScanShell(Long scanDefinitionId)
	{
		ScanDefinition definition = this.modelDelegate.getScanDefinitionById(scanDefinitionId);
		if (definition == null)
		{
			return null;
		}
		Scan scan = this.modelDelegate.openScan(definition);
		return scan.getId();
	}

	/**
	 * Worker-thread entrypoint. Runs the comparison work, fires the
	 * lifecycle notifications, and clears the in-flight registry slot
	 * regardless of outcome.
	 */
	void runScan(Long scanDefinitionId, Long scanId, boolean merge)
	{
		fireScanStarted(scanId);
		// A fresh thread pool is allocated per scan run. The legacy Swing UI
		// rebuilt its Spring context (and therefore its ThreadPool bean) per
		// scan; the IPC layer keeps a single context for the engine's
		// lifetime, so we must take responsibility for the lifecycle here.
		// Reusing a single AssuranceThreadPool across scans accumulates
		// Phaser state and causes the second await() to block indefinitely.
		IAssuranceThreadPool threadPool = this.scanThreadPoolFactory.get();
		try
		{
			IScanOptions options = this.modelDelegate.getScanOptions();
			IpcProgressMonitor monitor = new IpcProgressMonitor(this.notifier, scanId);
			Scan finished = this.modelDelegate.completeScan(scanId, threadPool, options, monitor);
			int resultsCount = (finished == null || finished.getUnmodifiableResults() == null)
				? 0 : finished.getUnmodifiableResults().size();
			fireScanCompleted(scanId, resultsCount);
			if (merge)
			{
				// Dispatch the post-scan merge through the same entrypoint a
				// History-tab Resolve would hit. It enqueues on mutationExecutor
				// (separate from scanExecutor), fires its own merge* lifecycle
				// notifications, and the renderer's mergeScanReducer handles a
				// STARTED arriving without a prior REQUEST by creating a
				// synthetic running entry.
				StartWholeScanMergeOutcome mergeOutcome = startWholeScanMerge(scanId);
				if (mergeOutcome.getStatus() != StartWholeScanMergeStatus.STARTED)
				{
					logger.warn("Auto-merge after scan {} did not start: {}", scanId, mergeOutcome.getStatus());
				}
			}
		}
		catch (AssuranceNullFileReferenceException | AssuranceIncompleteScanDefinitionException ex)
		{
			logger.error("Scan " + scanId + " failed validation", ex);
			fireScanFailed(scanId, ex);
		}
		catch (RuntimeException ex)
		{
			logger.error("Scan " + scanId + " failed", ex);
			fireScanFailed(scanId, ex);
		}
		finally
		{
			threadPool.shutdown();
			this.scanRunRegistry.release(scanDefinitionId);
		}
	}

	private void fireScanStarted(Long scanId)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("scanId", scanId);
		params.put("startedAt", Instant.now().toString());
		active.publish("assurance.scanStarted", params);
	}

	private void fireScanCompleted(Long scanId, int resultsCount)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("scanId", scanId);
		params.put("completedAt", Instant.now().toString());
		params.put("resultCount", resultsCount);
		active.publish("assurance.scanCompleted", params);
	}

	private void fireScanFailed(Long scanId, Throwable ex)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("scanId", scanId);
		params.put("failedAt", Instant.now().toString());
		ObjectNode error = JsonNodeFactory.instance.objectNode();
		error.put("code", RpcErrorCodes.ENGINE_ERROR);
		error.put("message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
		params.set("error", error);
		active.publish("assurance.scanFailed", params);
	}

	/**
	 * Result status of {@link #startScanResultMerge(Long, AssuranceMergeStrategy) startScanResultMerge}.
	 * The handler maps {@link #STARTED} to a success response carrying the
	 * {@code resultId} and parent {@code scanId};
	 * {@link #RESULT_NOT_FOUND} to JSON-RPC {@code -32002}; and
	 * {@link #ALREADY_RUNNING} to JSON-RPC {@code -32007}.
	 */
	public enum StartResultMergeStatus
	{
		STARTED,
		RESULT_NOT_FOUND,
		ALREADY_RUNNING,
	}

	public static final class StartResultMergeOutcome
	{
		private final StartResultMergeStatus status;
		private final Long resultId;
		private final Long scanId;

		private StartResultMergeOutcome(StartResultMergeStatus status, Long resultId, Long scanId)
		{
			this.status = status;
			this.resultId = resultId;
			this.scanId = scanId;
		}

		public StartResultMergeStatus getStatus()
		{
			return status;
		}

		public Long getResultId()
		{
			return resultId;
		}

		public Long getScanId()
		{
			return scanId;
		}
	}

	/**
	 * Resolves {@code resultId} to its parent {@code scanId} and submits a
	 * single-result merge for asynchronous execution. Returns immediately;
	 * progress is reported via {@code assurance.resultMerge*} notifications.
	 *
	 * <p>The strategy is mandatory; the handler validates it against
	 * {@link AssuranceMergeStrategy} before calling in.
	 *
	 * @return outcome carrying {@code resultId} + parent {@code scanId} on
	 *   success, or a non-{@code STARTED} status when the request was
	 *   rejected
	 */
	public StartResultMergeOutcome startScanResultMerge(Long resultId, AssuranceMergeStrategy strategy)
	{
		if (resultId == null)
		{
			return new StartResultMergeOutcome(StartResultMergeStatus.RESULT_NOT_FOUND, null, null);
		}
		Long parentScanId = lookupParentScanId(resultId);
		if (parentScanId == null)
		{
			return new StartResultMergeOutcome(StartResultMergeStatus.RESULT_NOT_FOUND, resultId, null);
		}
		if (!this.resultMergeRegistry.tryAcquire(resultId, resultId))
		{
			return new StartResultMergeOutcome(StartResultMergeStatus.ALREADY_RUNNING, resultId, parentScanId);
		}

		this.mutationExecutor.submit(() -> runResultMerge(resultId, parentScanId, strategy));
		return new StartResultMergeOutcome(StartResultMergeStatus.STARTED, resultId, parentScanId);
	}

	@Transactional(readOnly = true)
	protected Long lookupParentScanId(Long resultId)
	{
		ComparisonResult result = this.modelDelegate.getResultById(resultId);
		if (result == null)
		{
			return null;
		}
		Scan parent = result.getScan();
		return parent == null ? null : parent.getId();
	}

	/**
	 * Worker-thread entrypoint for a single-result merge. Runs the merge
	 * work, fires the lifecycle notifications, and clears the in-flight
	 * registry slot regardless of outcome.
	 */
	void runResultMerge(Long resultId, Long parentScanId, AssuranceMergeStrategy strategy)
	{
		fireResultMergeStarted(resultId, parentScanId);
		try
		{
			IpcResultProgressMonitor monitor = new IpcResultProgressMonitor(this.notifier, resultId, parentScanId);
			invokeResultMerge(resultId, strategy, monitor);
			fireResultMergeCompleted(resultId, parentScanId);
		}
		catch (RuntimeException ex)
		{
			logger.error("Result merge for result " + resultId + " failed", ex);
			fireResultMergeFailed(resultId, parentScanId, ex);
		}
		finally
		{
			this.resultMergeRegistry.release(resultId);
		}
	}

	@Transactional
	protected void invokeResultMerge(Long resultId, AssuranceMergeStrategy strategy, IpcResultProgressMonitor monitor)
	{
		ComparisonResult managed = this.modelDelegate.getResultById(resultId);
		if (managed == null)
		{
			throw new IllegalStateException("No comparison result found with id " + resultId);
		}
		try
		{
			this.modelDelegate.mergeScanResult(managed, strategy, monitor);
		}
		catch (AssuranceNullFileReferenceException ex)
		{
			throw new RuntimeException("Merge failed for result " + resultId, ex);
		}
	}

	private void fireResultMergeStarted(Long resultId, Long scanId)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("resultId", resultId);
		params.put("scanId", scanId);
		params.put("startedAt", Instant.now().toString());
		active.publish("assurance.resultMergeStarted", params);
	}

	private void fireResultMergeCompleted(Long resultId, Long scanId)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("resultId", resultId);
		params.put("scanId", scanId);
		params.put("completedAt", Instant.now().toString());
		active.publish("assurance.resultMergeCompleted", params);
	}

	private void fireResultMergeFailed(Long resultId, Long scanId, Throwable ex)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("resultId", resultId);
		params.put("scanId", scanId);
		params.put("failedAt", Instant.now().toString());
		ObjectNode error = JsonNodeFactory.instance.objectNode();
		error.put("code", RpcErrorCodes.ENGINE_ERROR);
		error.put("message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
		params.set("error", error);
		active.publish("assurance.resultMergeFailed", params);
	}

	/**
	 * Result status of {@link #startWholeScanMerge(Long) startWholeScanMerge}.
	 * The handler maps {@link #STARTED} to a success response carrying the
	 * {@code scanId}; {@link #SCAN_NOT_FOUND} to JSON-RPC {@code -32002}; and
	 * {@link #ALREADY_RUNNING} to JSON-RPC {@code -32006}.
	 */
	public enum StartWholeScanMergeStatus
	{
		STARTED,
		SCAN_NOT_FOUND,
		ALREADY_RUNNING,
	}

	public static final class StartWholeScanMergeOutcome
	{
		private final StartWholeScanMergeStatus status;
		private final Long scanId;

		private StartWholeScanMergeOutcome(StartWholeScanMergeStatus status, Long scanId)
		{
			this.status = status;
			this.scanId = scanId;
		}

		public StartWholeScanMergeStatus getStatus()
		{
			return status;
		}

		public Long getScanId()
		{
			return scanId;
		}
	}

	/**
	 * Submits a whole-scan merge for asynchronous execution. Returns
	 * immediately; progress is reported via {@code assurance.merge*}
	 * notifications bound to the same {@code scanId}.
	 *
	 * <p>The strategy is taken from the scan's owning {@link ScanDefinition}
	 * (see {@link com.markallenjohnson.assurance.model.ModelDelegate#mergeScan}),
	 * mirroring the legacy 1.x semantics where "Scan and Merge" used the
	 * definition-level merge strategy and the wire never had to carry one.
	 *
	 * @return outcome carrying {@code scanId} on success, or a non-{@code STARTED}
	 *   status when the request was rejected
	 */
	public StartWholeScanMergeOutcome startWholeScanMerge(Long scanId)
	{
		if (scanId == null)
		{
			return new StartWholeScanMergeOutcome(StartWholeScanMergeStatus.SCAN_NOT_FOUND, null);
		}
		if (!scanExists(scanId))
		{
			return new StartWholeScanMergeOutcome(StartWholeScanMergeStatus.SCAN_NOT_FOUND, scanId);
		}
		if (!this.wholeScanMergeRegistry.tryAcquire(scanId, scanId))
		{
			return new StartWholeScanMergeOutcome(StartWholeScanMergeStatus.ALREADY_RUNNING, scanId);
		}

		this.mutationExecutor.submit(() -> runWholeScanMerge(scanId));
		return new StartWholeScanMergeOutcome(StartWholeScanMergeStatus.STARTED, scanId);
	}

	@Transactional(readOnly = true)
	protected boolean scanExists(Long scanId)
	{
		return this.modelDelegate.getScanById(scanId) != null;
	}

	/**
	 * Worker-thread entrypoint for a whole-scan merge. Fires the lifecycle
	 * notifications and clears the in-flight registry slot regardless of
	 * outcome. Allocates a per-run thread pool because
	 * {@link com.markallenjohnson.assurance.model.ModelDelegate#mergeScan}
	 * calls {@code threadPool.await()} internally; reusing a singleton would
	 * accumulate Phaser state and hang on the second run (see the T3.6
	 * follow-up note in {@code tasks/todo.md}).
	 */
	void runWholeScanMerge(Long scanId)
	{
		fireMergeStarted(scanId);
		IAssuranceThreadPool threadPool = this.scanThreadPoolFactory.get();
		try
		{
			IpcMergeProgressMonitor monitor = new IpcMergeProgressMonitor(this.notifier, scanId);
			int itemsMerged = invokeWholeScanMerge(scanId, threadPool, monitor);
			fireMergeCompleted(scanId, itemsMerged);
		}
		catch (RuntimeException ex)
		{
			logger.error("Whole-scan merge for scan " + scanId + " failed", ex);
			fireMergeFailed(scanId, ex);
		}
		finally
		{
			threadPool.shutdown();
			this.wholeScanMergeRegistry.release(scanId);
		}
	}

	int invokeWholeScanMerge(Long scanId, IAssuranceThreadPool threadPool, IpcMergeProgressMonitor monitor)
	{
		// itemsMerged is the delta of resolved results across the merge call:
		// any result the engine actually touched in this run transitions from
		// UNRESOLVED to a non-UNRESOLVED resolution (REPLACE_TARGET, DELETE_TARGET,
		// PROCESSING_ERROR_ENCOUNTERED, …). Sandwiching the count around mergeScan
		// excludes results that were already resolved by a prior merge and
		// excludes results MergeEngine.shouldMerge skipped.
		int countBefore = this.modelDelegate.countResolvedComparisonResultsForScan(scanId);
		Scan managed = this.modelDelegate.getScanById(scanId);
		if (managed == null)
		{
			throw new IllegalStateException("No scan found with id " + scanId);
		}
		try
		{
			this.modelDelegate.mergeScan(managed, threadPool, monitor);
		}
		catch (AssuranceNullFileReferenceException ex)
		{
			throw new RuntimeException("Whole-scan merge failed for scan " + scanId, ex);
		}
		int countAfter = this.modelDelegate.countResolvedComparisonResultsForScan(scanId);
		return Math.max(0, countAfter - countBefore);
	}

	private void fireMergeStarted(Long scanId)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("scanId", scanId);
		params.put("startedAt", Instant.now().toString());
		active.publish("assurance.mergeStarted", params);
	}

	private void fireMergeCompleted(Long scanId, int itemsMerged)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("scanId", scanId);
		params.put("completedAt", Instant.now().toString());
		params.put("itemsMerged", itemsMerged);
		active.publish("assurance.mergeCompleted", params);
	}

	private void fireMergeFailed(Long scanId, Throwable ex)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("scanId", scanId);
		params.put("failedAt", Instant.now().toString());
		ObjectNode error = JsonNodeFactory.instance.objectNode();
		error.put("code", RpcErrorCodes.ENGINE_ERROR);
		error.put("message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
		params.set("error", error);
		active.publish("assurance.mergeFailed", params);
	}

	/**
	 * Result status of {@link #startRestoreDeletedItem(Long) startRestoreDeletedItem}.
	 * The handler maps {@link #STARTED} to a success response carrying the
	 * {@code resultId} and parent {@code scanId};
	 * {@link #RESULT_NOT_FOUND} to JSON-RPC {@code -32002}; and
	 * {@link #ALREADY_RUNNING} to JSON-RPC {@code -32008}.
	 */
	public enum StartRestoreStatus
	{
		STARTED,
		RESULT_NOT_FOUND,
		ALREADY_RUNNING,
	}

	public static final class StartRestoreOutcome
	{
		private final StartRestoreStatus status;
		private final Long resultId;
		private final Long scanId;

		private StartRestoreOutcome(StartRestoreStatus status, Long resultId, Long scanId)
		{
			this.status = status;
			this.resultId = resultId;
			this.scanId = scanId;
		}

		public StartRestoreStatus getStatus()
		{
			return status;
		}

		public Long getResultId()
		{
			return resultId;
		}

		public Long getScanId()
		{
			return scanId;
		}
	}

	/**
	 * Resolves {@code resultId} to its parent {@code scanId} and submits a
	 * single-result restore for asynchronous execution. Returns immediately;
	 * progress is reported via {@code assurance.restore*} notifications.
	 *
	 * <p>The {@link com.markallenjohnson.assurance.model.ModelDelegate#restoreDeletedItem}
	 * implementation chooses the strategy from the result's
	 * {@link com.markallenjohnson.assurance.model.enums.AssuranceResultResolution};
	 * the wire never carries one (mirroring the legacy 1.x semantics).
	 *
	 * @return outcome carrying {@code resultId} + parent {@code scanId} on
	 *   success, or a non-{@code STARTED} status when the request was
	 *   rejected
	 */
	public StartRestoreOutcome startRestoreDeletedItem(Long resultId)
	{
		if (resultId == null)
		{
			return new StartRestoreOutcome(StartRestoreStatus.RESULT_NOT_FOUND, null, null);
		}
		Long parentScanId = lookupParentScanId(resultId);
		if (parentScanId == null)
		{
			return new StartRestoreOutcome(StartRestoreStatus.RESULT_NOT_FOUND, resultId, null);
		}
		if (!this.restoreRegistry.tryAcquire(resultId, resultId))
		{
			return new StartRestoreOutcome(StartRestoreStatus.ALREADY_RUNNING, resultId, parentScanId);
		}

		this.mutationExecutor.submit(() -> runRestoreDeletedItem(resultId, parentScanId));
		return new StartRestoreOutcome(StartRestoreStatus.STARTED, resultId, parentScanId);
	}

	/**
	 * Worker-thread entrypoint for a single-result restore. Runs the restore
	 * work, fires the lifecycle notifications, and clears the in-flight
	 * registry slot regardless of outcome.
	 */
	void runRestoreDeletedItem(Long resultId, Long parentScanId)
	{
		fireRestoreStarted(resultId, parentScanId);
		try
		{
			IpcRestoreProgressMonitor monitor = new IpcRestoreProgressMonitor(this.notifier, resultId, parentScanId);
			invokeRestoreDeletedItem(resultId, monitor);
			fireRestoreCompleted(resultId, parentScanId);
		}
		catch (RuntimeException ex)
		{
			logger.error("Restore for result " + resultId + " failed", ex);
			fireRestoreFailed(resultId, parentScanId, ex);
		}
		finally
		{
			this.restoreRegistry.release(resultId);
		}
	}

	@Transactional
	protected void invokeRestoreDeletedItem(Long resultId, IpcRestoreProgressMonitor monitor)
	{
		ComparisonResult managed = this.modelDelegate.getResultById(resultId);
		if (managed == null)
		{
			throw new IllegalStateException("No comparison result found with id " + resultId);
		}
		this.modelDelegate.restoreDeletedItem(managed, monitor);
	}

	private void fireRestoreStarted(Long resultId, Long scanId)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("resultId", resultId);
		params.put("scanId", scanId);
		params.put("startedAt", Instant.now().toString());
		active.publish("assurance.restoreStarted", params);
	}

	private void fireRestoreCompleted(Long resultId, Long scanId)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("resultId", resultId);
		params.put("scanId", scanId);
		params.put("completedAt", Instant.now().toString());
		active.publish("assurance.restoreCompleted", params);
	}

	private void fireRestoreFailed(Long resultId, Long scanId, Throwable ex)
	{
		IpcNotifier active = this.notifier;
		if (active == null)
		{
			return;
		}
		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("resultId", resultId);
		params.put("scanId", scanId);
		params.put("failedAt", Instant.now().toString());
		ObjectNode error = JsonNodeFactory.instance.objectNode();
		error.put("code", RpcErrorCodes.ENGINE_ERROR);
		error.put("message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
		params.set("error", error);
		active.publish("assurance.restoreFailed", params);
	}

	/**
	 * Stops accepting new async work. Existing in-flight scans are allowed to
	 * complete during shutdown, bounded by a small grace period; anything
	 * still running after the timeout is interrupted. Idempotent.
	 */
	public void shutdown()
	{
		shutdownExecutor(this.scanExecutor);
		shutdownExecutor(this.mutationExecutor);
	}

	private static void shutdownExecutor(ExecutorService executor)
	{
		executor.shutdown();
		try
		{
			if (!executor.awaitTermination(5, TimeUnit.SECONDS))
			{
				executor.shutdownNow();
			}
		}
		catch (InterruptedException ex)
		{
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private static final AtomicInteger SCAN_THREAD_INDEX = new AtomicInteger(0);
	private static final AtomicInteger MUTATION_THREAD_INDEX = new AtomicInteger(0);

	private static Thread namedScanThread(Runnable runnable)
	{
		Thread t = new Thread(runnable, "assurance-scan-runner-" + SCAN_THREAD_INDEX.getAndIncrement());
		t.setDaemon(true);
		return t;
	}

	private static Thread namedMutationThread(Runnable runnable)
	{
		Thread t = new Thread(runnable, "assurance-mutation-runner-" + MUTATION_THREAD_INDEX.getAndIncrement());
		t.setDaemon(true);
		return t;
	}

	/**
	 * Bridges the engine's {@link com.markallenjohnson.assurance.notification.IProgressMonitor}
	 * callbacks onto JSON-RPC {@code assurance.scanProgress} notifications.
	 * The {@code chunk} payload from the engine is currently
	 * type-erased ({@code Object}) and stringified by the existing Swing
	 * worker; we mirror that for now and ship a structured shape later when
	 * the engine emits one.
	 */
	static final class IpcProgressMonitor implements com.markallenjohnson.assurance.notification.IProgressMonitor
	{
		private final IpcNotifier notifier;
		private final Long scanId;

		IpcProgressMonitor(IpcNotifier notifier, Long scanId)
		{
			this.notifier = notifier;
			this.scanId = scanId;
		}

		@Override
		public void publish(Object chunk)
		{
			IpcNotifier active = this.notifier;
			if (active == null)
			{
				return;
			}
			ObjectNode params = JsonNodeFactory.instance.objectNode();
			params.put("scanId", scanId);
			ObjectNode progress = JsonNodeFactory.instance.objectNode();
			progress.put("phase", "scanning");
			progress.put("currentItem", chunk == null ? "" : chunk.toString());
			params.set("progress", progress);
			active.publish("assurance.scanProgress", params);
		}
	}

	/**
	 * Bridges {@link com.markallenjohnson.assurance.notification.IProgressMonitor}
	 * callbacks onto JSON-RPC {@code assurance.resultMergeProgress}
	 * notifications. Mirrors {@link IpcProgressMonitor}'s opaque
	 * {@code currentItem}-only shape; we tighten it as the engine starts
	 * emitting structured progress chunks.
	 */
	static final class IpcResultProgressMonitor implements com.markallenjohnson.assurance.notification.IProgressMonitor
	{
		private final IpcNotifier notifier;
		private final Long resultId;
		private final Long scanId;

		IpcResultProgressMonitor(IpcNotifier notifier, Long resultId, Long scanId)
		{
			this.notifier = notifier;
			this.resultId = resultId;
			this.scanId = scanId;
		}

		@Override
		public void publish(Object chunk)
		{
			IpcNotifier active = this.notifier;
			if (active == null)
			{
				return;
			}
			ObjectNode params = JsonNodeFactory.instance.objectNode();
			params.put("resultId", resultId);
			params.put("scanId", scanId);
			ObjectNode progress = JsonNodeFactory.instance.objectNode();
			progress.put("phase", "merging");
			progress.put("currentItem", chunk == null ? "" : chunk.toString());
			params.set("progress", progress);
			active.publish("assurance.resultMergeProgress", params);
		}
	}

	/**
	 * Bridges {@link com.markallenjohnson.assurance.notification.IProgressMonitor}
	 * callbacks onto JSON-RPC {@code assurance.mergeProgress} notifications
	 * for a whole-scan merge. Mirrors {@link IpcResultProgressMonitor} but
	 * carries only {@code scanId} (no {@code resultId}).
	 */
	static final class IpcMergeProgressMonitor implements com.markallenjohnson.assurance.notification.IProgressMonitor
	{
		private final IpcNotifier notifier;
		private final Long scanId;

		IpcMergeProgressMonitor(IpcNotifier notifier, Long scanId)
		{
			this.notifier = notifier;
			this.scanId = scanId;
		}

		@Override
		public void publish(Object chunk)
		{
			IpcNotifier active = this.notifier;
			if (active == null)
			{
				return;
			}
			ObjectNode params = JsonNodeFactory.instance.objectNode();
			params.put("scanId", scanId);
			ObjectNode progress = JsonNodeFactory.instance.objectNode();
			progress.put("phase", "merging");
			progress.put("currentItem", chunk == null ? "" : chunk.toString());
			params.set("progress", progress);
			active.publish("assurance.mergeProgress", params);
		}
	}

	/**
	 * Bridges {@link com.markallenjohnson.assurance.notification.IProgressMonitor}
	 * callbacks onto JSON-RPC {@code assurance.restoreProgress} notifications.
	 * Mirrors {@link IpcResultProgressMonitor} but with the restore-family
	 * method name and {@code phase = "restoring"}.
	 */
	static final class IpcRestoreProgressMonitor implements com.markallenjohnson.assurance.notification.IProgressMonitor
	{
		private final IpcNotifier notifier;
		private final Long resultId;
		private final Long scanId;

		IpcRestoreProgressMonitor(IpcNotifier notifier, Long resultId, Long scanId)
		{
			this.notifier = notifier;
			this.resultId = resultId;
			this.scanId = scanId;
		}

		@Override
		public void publish(Object chunk)
		{
			IpcNotifier active = this.notifier;
			if (active == null)
			{
				return;
			}
			ObjectNode params = JsonNodeFactory.instance.objectNode();
			params.put("resultId", resultId);
			params.put("scanId", scanId);
			ObjectNode progress = JsonNodeFactory.instance.objectNode();
			progress.put("phase", "restoring");
			progress.put("currentItem", chunk == null ? "" : chunk.toString());
			params.set("progress", progress);
			active.publish("assurance.restoreProgress", params);
		}
	}
}
