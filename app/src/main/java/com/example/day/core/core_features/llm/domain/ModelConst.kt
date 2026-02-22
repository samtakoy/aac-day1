package com.example.day.core.core_features.llm.domain

object ModelConst {

    // upstage/solar-pro-3:free
    // stepfun/step-3.5-flash:free
    // deepseek/deepseek-r1-0528:free .  + но очень долго думает
    // mistralai/mistral-small-3.1-24b-instruct:free . -
    // qwen/qwen3-next-80b-a3b-instruct:free
    // qwen/qwen3-coder:free
    // arcee-ai/trinity-mini:free
    // .openai/gpt-oss-20b:free
    // google/gemma-3n-e4b-it:free
    // nousresearch/hermes-3-llama-3.1-405b:free
    // google/gemma-3-12b-it:free
    // meta-llama/llama-3.3-70b-instruct:free

    // nvidia/nemotron-3-nano-30b-a3b:free . +
    // z-ai/glm-4.5-air:free .  +

    // openai/gpt-4o-mini
    // meta-llama/llama-3.1-8b-instruct  - туповатая
    // google/gemini-2.0-flash-lite-001 - блокнутая
    // openai/gpt-3.5-turbo  - типа работает
    // openai/gpt-4.1  - плыветА
    // z-ai/glm-4.5-air
    // meta-llama/llama-3.3-70b-instruct
    const val DEFAULT_MODEL = "meta-llama/llama-3.3-70b-instruct"
}