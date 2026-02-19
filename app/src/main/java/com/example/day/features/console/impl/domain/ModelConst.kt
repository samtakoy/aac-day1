package com.example.day.features.console.impl.domain

object ModelConst {
    // z-ai/glm-4.5-air:free .  +
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
    const val DEFAULT_MODEL = "nvidia/nemotron-3-nano-30b-a3b:free"
    const val MAX_ITERATIONS = 5
}