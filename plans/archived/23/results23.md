Интерпретация результатов — День 23
Сводная таблица по пресетам
Пресет	Avg top score	Retrieval TopK	Threshold	Reranker	Final TopK
BASELINE	0.67	10	0.0 (выкл)	NONE	5
FILTERED	0.69	15	0.5	NONE	5
RERANKED_HEURISTIC	0.73	15	0.5	HEURISTIC	5
RERANKED_LLM	0.93	15	0.5	LLM	5
Интерпретация по шагам
BASELINE → FILTERED (+0.02) — маленький прирост.

Порог 0.5 практически ничего не отфильтровал — метрики Query 1 показывают Retrieved: 10 → Filtered: 10. Это нормально для кодовой базы: embeddings достаточно похожи, шума мало. Польза фильтра — в защите от совсем случайных результатов, а не в росте avg score.
FILTERED → RERANKED_HEURISTIC (+0.04) — заметный прирост.

Keyword overlap реально переставляет порядок результатов. Query 1 показывает: с baseline AgentLifecycleHandlersCollector на первом месте (score 0.66), после heuristic rerank — AIAgentService (0.70). Ответ стал точнее по содержанию.
RERANKED_HEURISTIC → RERANKED_LLM (+0.20) — очень значительный прирост.

LLM reranker выдаёт 0.93 avg top score. Query 1: на первом месте AIAgentFeature (0.85) вместо 0.66 baseline. LLM правильно понял запрос и поставил вперёд интерфейс, а не вспомогательный collector.
Стоимость: rerank=7545ms — 7.5 секунд на реранкинг. Это медленно для интерактивного чата, приемлемо для фонового поиска.
Достигнуты ли цели задачи?
Требование задачи	Статус
Reranker или фильтр релевантности	✅ Реализованы оба: ThresholdFilter + HeuristicReranker + LlmReranker
Порог отсечения нерелевантных	✅ threshold=0.5, настраивается через query param
Топ-K до и после фильтрации	✅ Метрики Retrieved → Filtered → Final видны в каждом запросе
Query rewrite	✅ QueryOptimizer работает (виден Optimized: в каждом отчёте)
Сравнение без фильтра vs с фильтром	✅ 4 пресета с отчётами позволяют сравнивать
Вывод: все требования Дня 23 выполнены.

Ключевые наблюдения
Query Optimizer дал наибольший базовый эффект — русские запросы переводятся в английские технические ("какие возможности агента" → "what are the main agent capabilities, which class implements"). Без этого baseline был бы ≈0.49 (как в verif2.md).

LLM reranker — победитель по качеству (0.93), но медленный (~7.5s). Для production-сценария лучше подошёл бы Cross-Encoder (ONNX BGE-Reranker), который в задании отмечен как приоритет, но не реализован — это единственный незакрытый пункт плана.

Heuristic reranker — хороший баланс скорости (2ms) и прироста (+0.06 к baseline). Годится как дефолт.

Threshold 0.5 слишком мягкий для этой кодовой базы (все 10 результатов проходят). Стоит попробовать 0.60–0.65 для реального отсечения шума.





Сравнение качества ответов LLM: BASELINE vs RERANKED_LLM
Q1: "какие основные возможности агента, какой класс реализует"
BASELINE — корректный, но поверхностный ответ. Упоминает AIAgentFeature, перечисляет примеры (планирование, инструменты, логирование), но выводит AgentLifecycleHandlersCollector как ключевой класс — это вспомогательный сборщик, а не основной.

RERANKED_LLM — точнее. Чётко выделяет AIAgentFeature как базовый интерфейс, конкретизирует специализации (AIAgentGraphFeature, AIAgentFunctionalFeature, AIAgentPlannerFeature) с указанием типов конвейеров. Структурированнее.

Победитель: RERANKED_LLM (небольшое преимущество)

Q2: "как конфигурируется агент"
BASELINE — нашёл AIAgentHelper.builder() паттерн. Хороший пошаговый ответ с примерами кода (7 шагов), таблица ключевых классов.

RERANKED_LLM — нашёл механизм install {} + FunctionalAIAgent/PlannerAIAgent. Покрывает FeatureMessageProcessor, фильтрацию событий, setEventFilter. Другой угол зрения, но более актуальный для feature-based архитектуры.

Победитель: ничья — каждый ответ нашёл реальный, но разный аспект API.

Q3: "как агент работает с историей сообщений"
BASELINE — покрывает все механизмы: AgentContextData, ChatMemory, стратегии сжатия, rollback. Структурно хорошо.

RERANKED_LLM — то же самое, но более сжато. Дополнительно упоминает singleRunStrategyWithHistoryCompression (автоматический триггер сжатия). Немного компактнее.

Победитель: ничья с лёгким преимуществом RERANKED_LLM за singleRunStrategyWithHistoryCompression

Q4: "как реализованы стратегии компактизации контекста"
BASELINE — признаёт ограничения: "Контекст класса не включает полную реализацию конкретных стратегий, лишь их названия". Описание размытое, без деталей реализации каждой стратегии.

RERANKED_LLM — конкретные примеры для каждой стратегии:


WholeHistory: [System, User, Assistant, ToolCall] → [System, User, Memory, TLDR]
WholeHistoryMultipleSystemMessages: разбивает на блоки по границам system messages
FromLastNMessages(n): сохраняет только n последних
FromTimestamp: фильтрует по времени
Объясняет механизм TL;DR через LLM, интеграцию с AIAgentLLMWriteSession.

Победитель: RERANKED_LLM убедительно — разница в глубине ответа принципиальная.

Q5: "как реализован tool calling"
BASELINE — описывает ReceivedToolResult, ToolCallDescriber, одно событие (ToolCallStartingEvent), 6-шаговый поток. Базовое покрытие.

RERANKED_LLM — дополнительно: McpTool адаптер с деталями конвертации аргументов, 4 события (Starting, Completed, Failed, ValidationFailed), ToolCallEventContext со всеми полями, секция обработки ошибок, раздел оптимизации токенов в промпте.

Победитель: RERANKED_LLM убедительно

Итог сравнения
Вопрос	BASELINE	RERANKED_LLM
Q1: возможности агента	3/5	4/5
Q2: конфигурация	4/5	4/5
Q3: история сообщений	4/5	4/5
Q4: стратегии компактизации	2/5	5/5
Q5: tool calling	3/5	5/5
Среднее	3.2	4.4
Разница наиболее заметна на вопросах с деталями реализации (Q4, Q5) — именно там LLM reranker вытащил более глубокие чанки из кодовой базы.

По вопросу о переводе запросов
Да, перевод включён во всех 4 пресетах — это видно по строке Optimized: в каждом eval-отчёте:


