# General Workflow Approach

The current recommended strategy for working with AI is inspired by [Harper Reed's LLM workflow](https://harper.blog/2025/02/16/my-llm-codegen-workflow-atm/) and [his followup revisions](https://harper.blog/2025/04/17/an-llm-codegen-heros-journey/).

## Greenfield Development

### Idea Honing
- Use a conversational LLM to hone in on an idea
```
Ask me one question at a time so we can develop a thorough, step-by-step spec for this idea. Each question should build on my previous answers, and our end goal is to have a detailed specification I can hand off to a developer. Let’s do this iteratively and dig into every relevant detail. Remember, only one question at a time.

Here’s the idea:

<IDEA>
```
- Create specification
```
Now that we’ve wrapped up the brainstorming process, can you compile our findings into a comprehensive, developer-ready specification? Include all relevant requirements, architecture choices, data handling details, error handling strategies, and a testing plan so a developer can immediately begin implementation.
```
- Save to `spec.md`

### Planning
- Take the spec and pass it to a proper reasoning model
```
Draft a detailed, step-by-step blueprint for building this project. Then, once you have a solid plan, break it down into small, iterative chunks that build on each other. Look at these chunks and then go another round to break it into small steps. Review the results and make sure that the steps are small enough to be implemented safely with strong testing, but big enough to move the project forward. Iterate until you feel that the steps are right sized for this project.

From here you should have the foundation to provide a series of prompts for a code-generation LLM that will implement each step in a test-driven manner. Prioritize best practices, incremental progress, and early testing, ensuring no big jumps in complexity at any stage. Make sure that each prompt builds on the previous prompts, and ends with wiring things together. There should be no hanging or orphaned code that isn't integrated into a previous step.

Make sure and separate each prompt section. Use markdown. Each prompt should be tagged as text using code tags. The goal is to output prompts, but context, etc is important as well.

<SPEC>
```
- Save to `prompt_plan.md`
- Generate a TODO for progress tracking
```
Can you make a `todo.md` that I can use as a checklist? Be thorough.
```
- Save to `todo.md`

### Execution
- Effectively pair program with a codegen tool, dropping each prompt in iteratively.
- set up the repo
- paste prompt into codegen tool
- run code, run tests
- if it works, move on to next prompt
- if it doesn't work, debug with the codegen tool, passing as much context about the codebase as possible

## Non-Greenfield Development

### Get Context
- navigate to code repo
- run [LLM Mise tasks](mise.toml) to perform the targeted LLM operation to generate prompts and context

### Prompt Magic
- paste prompts back into codegen tool
- Use pre-defined prompts to analyze and modify established code base
#### Code Review
```
You are a senior developer. Your job is to do a thorough code review of this code. You should write it up and output markdown. Include line numbers, and contextual info. Your code review will be passed to another teammate, so be thorough. Think deeply  before writing the code review. Review every part, and don't hallucinate.
```
##### GitHub Issue Generation
```
You are a senior developer. Your job is to review this code, and write out the top issues that you see with the code. It could be bugs, design choices, or code cleanliness issues. You should be specific, and be very good. Do Not Hallucinate. Think quietly to yourself, then act - write the issues. The issues will be given to a developer to executed on, so they should be in a format that is compatible with github issues
```
#### Missing Tests
```
You are a senior developer. Your job is to review this code, and write out a list of missing test cases, and code tests that should exist. You should be specific, and be very good. Do Not Hallucinate. Think quietly to yourself, then act - write the issues. The issues  will be given to a developer to executed on, so they should be in a format that is compatible with github issues
```
