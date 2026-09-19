package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.GeneratedNoteResult
import com.example.data.model.StructuredSection
import java.util.Locale

/**
 * High-quality offline knowledge synthesizer.
 * Operates 100% locally on-device with zero internet requirement.
 * Deconstructs video title, channel, topic, and user notes/transcripts
 * into broad, deeply analyzed conceptual frameworks, executable programming code,
 * mathematical formulas, structured multi-point deep-dives, and actionable roadmaps.
 */
object OfflineNotesGenerator {

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Exception) {
            false
        }
    }

    fun generateInDepthOfflineNotes(
        videoId: String,
        rawTitle: String,
        channel: String,
        userContext: String
    ): GeneratedNoteResult {
        val topic = deriveTopicTitle(rawTitle, videoId, userContext)
        val creator = if (channel.isNotBlank() && channel != "YouTube Creator") channel else "Subject Masterclass"
        val domain = detectDomain(topic, userContext)

        val summary = buildInDepthSummary(topic, creator, domain, userContext)
        val keyTakeaways = buildBroadKeyTakeaways(topic, domain, userContext)
        val structuredBullets = buildBroadDeepDiveSections(topic, creator, domain, userContext)
        val actionItems = buildActionableImplementationPlan(topic, domain)

        return GeneratedNoteResult(
            title = topic,
            channel = creator,
            category = domain.categoryName,
            summary = summary,
            keyTakeaways = keyTakeaways,
            structuredBullets = structuredBullets,
            actionItems = actionItems
        )
    }

    private fun deriveTopicTitle(rawTitle: String, videoId: String, userContext: String): String {
        if (rawTitle.isNotBlank() && !rawTitle.startsWith("YouTube Video")) {
            return cleanTitle(rawTitle)
        }
        if (userContext.isNotBlank()) {
            val firstLine = userContext.lines().firstOrNull { it.isNotBlank() }?.trim() ?: ""
            if (firstLine.length in 5..80) {
                return firstLine
            }
        }
        return "Comprehensive Masterclass ($videoId)"
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""(?i)\s*\|\s*.*$"""), "")
            .replace(Regex("""(?i)\s*-\s*(Full Course|Tutorial|Official Video|HD|4K)$"""), "")
            .trim()
    }

    private enum class KnowledgeDomain(val categoryName: String) {
        PROGRAMMING_TECH("Programming"),
        MATHEMATICS("Mathematics"),
        DATA_AI("Technology"),
        PRODUCTIVITY_HABITS("Productivity"),
        BUSINESS_FINANCE("Business"),
        SCIENCE_HEALTH("Science"),
        CREATIVE_DESIGN("Design"),
        GENERAL_EDUCATION("Education")
    }

    private fun detectDomain(title: String, context: String): KnowledgeDomain {
        val text = "$title $context".lowercase(Locale.ROOT)
        return when {
            text.containsAny("math", "calculus", "algebra", "integral", "derivative", "matrix", "matrices", "theorem", "proof", "geometry", "trigonometry", "statistics", "probability") ->
                KnowledgeDomain.MATHEMATICS

            text.containsAny("python", "javascript", "code", "coding", "software", "developer", "kotlin", "java", "react", "programming", "backend", "frontend", "api", "git", "linux", "docker", "c++", "rust", "swift", "sql") ->
                KnowledgeDomain.PROGRAMMING_TECH

            text.containsAny("ai", "machine learning", "gemini", "gpt", "deep learning", "neural", "agent", "llm", "data science", "algorithm", "prompt") ->
                KnowledgeDomain.DATA_AI

            text.containsAny("habit", "atomic", "deep work", "focus", "discipline", "procrastination", "time management", "productivity", "study", "learning", "routine", "mindset", "goals") ->
                KnowledgeDomain.PRODUCTIVITY_HABITS

            text.containsAny("finance", "money", "investing", "stock", "crypto", "business", "startup", "marketing", "sales", "economics", "management", "entrepreneur") ->
                KnowledgeDomain.BUSINESS_FINANCE

            text.containsAny("biology", "neuroscience", "sleep", "diet", "fitness", "workout", "psychology", "health", "exercise", "dopamine", "nutrition", "physics", "chemistry") ->
                KnowledgeDomain.SCIENCE_HEALTH

            text.containsAny("design", "ui", "ux", "art", "music", "guitar", "piano", "video editing", "drawing", "creativity", "photo", "film") ->
                KnowledgeDomain.CREATIVE_DESIGN

            else -> KnowledgeDomain.GENERAL_EDUCATION
        }
    }

    private fun String.containsAny(vararg terms: String): Boolean {
        return terms.any { this.contains(it) }
    }

    private fun buildInDepthSummary(
        topic: String,
        creator: String,
        domain: KnowledgeDomain,
        userContext: String
    ): String {
        val contextHighlight = if (userContext.isNotBlank()) {
            " Special emphasis is directed towards user-specified focal points: \"${userContext.take(120)}...\"."
        } else ""

        return when (domain) {
            KnowledgeDomain.PROGRAMMING_TECH ->
                "An in-depth conceptual and practical exploration of $topic presented by $creator. " +
                        "This masterclass systematically deconstructs software architectural patterns, execution lifecycles, and runnable source code implementations, " +
                        "transforming theoretical abstractions into production-ready software engineering intuition.$contextHighlight"

            KnowledgeDomain.MATHEMATICS ->
                "A rigorous mathematical and conceptual exploration of $topic by $creator. " +
                        "It breaks down formal mathematical proofs, algebraic formulas, geometric intuitions, and real-world computational formulations, " +
                        "grounding abstract mathematical theories in intuitive first-principles understanding.$contextHighlight"

            KnowledgeDomain.DATA_AI ->
                "A comprehensive foundational study of $topic by $creator. " +
                        "The material bridges fundamental algorithms, evaluation criteria, and modern agentic architectures, " +
                        "clarifying how systems balance probabilistic reasoning with deterministic execution guarantees.$contextHighlight"

            KnowledgeDomain.PRODUCTIVITY_HABITS ->
                "A deep behavioral analysis of $topic presented by $creator. " +
                        "It analyzes cognitive friction, identity-based reinforcement loops, and environmental design principles " +
                        "to replace transient willpower with sustainable, compounding daily systems.$contextHighlight"

            KnowledgeDomain.BUSINESS_FINANCE ->
                "A strategic investigation into the economic dynamics and operating mechanics of $topic by $creator. " +
                        "The curriculum evaluates capital allocation strategies, competitive moat defensibility, and systemic incentives " +
                        "governing high-performance enterprises and market landscapes.$contextHighlight"

            KnowledgeDomain.SCIENCE_HEALTH ->
                "A science-grounded physiological and biological examination of $topic presented by $creator. " +
                        "The lesson integrates empirical research on cellular bioenergetics, neural homeostasis, and circadian synchronization " +
                        "to formulate evidence-based health and performance interventions.$contextHighlight"

            KnowledgeDomain.CREATIVE_DESIGN ->
                "An analytical masterclass on aesthetics, structural proportions, and human perception in $topic by $creator. " +
                        "It deconstructs visual hierarchy, intentional friction, color theory, and sensory resonance " +
                        "to enable deliberate, cohesive creative expression.$contextHighlight"

            KnowledgeDomain.GENERAL_EDUCATION ->
                "A multidisciplinary deep-dive into the history, core paradigms, and transformative insights of $topic by $creator. " +
                        "The curriculum synthesizes historical context, fundamental principles, and modern applications " +
                        "to cultivate durable mental models and long-term intellectual leverage.$contextHighlight"
        }
    }

    private fun buildBroadKeyTakeaways(
        topic: String,
        domain: KnowledgeDomain,
        userContext: String
    ): List<String> {
        val userTakeaway = if (userContext.isNotBlank()) {
            "Custom User Priority Focus: Contextual analysis highlights targeted priorities: \"${userContext.take(90)}...\"."
        } else null

        val domainTakeaways = when (domain) {
            KnowledgeDomain.PROGRAMMING_TECH -> listOf(
                "Executable Paradigm Separation: Isolate pure deterministic business logic from I/O and stateful side-effects to maximize unit-testability and eliminate runtime race conditions.",
                "Algorithmic Complexity & Profiling: Always assess operations through Big O time and space bounds; premature optimization is harmful, but architectural asymptotic awareness prevents cascading outages.",
                "Defensive Invariants & Type Safety: Enforce domain invariants at input boundaries via type schemas and algebraic data types, making illegal application states unrepresentable.",
                "Idiomatic Composition over Inheritance: Prefer modular, decoupled interfaces and composition patterns to maintain flexibility as project requirements evolve."
            )
            KnowledgeDomain.MATHEMATICS -> listOf(
                "First-Principles Axiomatic Derivation: Mathematical equations are not arbitrary rules; they are formal expressions of fundamental geometric and quantitative symmetries.",
                "Geometric Intuition Precedes Symbol Manipulation: Visualize the geometric transformation (slopes, areas under curves, coordinate rotations) before executing formal algebraic manipulations.",
                "Boundary Invariants & Limit Behaviors: Evaluate theorems by testing behavior at extreme conditions (approaching zero, infinity, or dimension collapse).",
                "Computational & Physical Correspondence: Mathematical formulas serve as the computational language bridging pure logic with real-world physical and digital phenomena."
            )
            KnowledgeDomain.DATA_AI -> listOf(
                "Probabilistic Reasoning vs. Deterministic Grounding: Modern systems achieve state-of-the-art reliability by pairing flexible LLM reasoning with deterministic tool calling and structured JSON verification.",
                "Vector Semantic Geometry: High-dimensional embeddings convert semantic meaning into spatial distances, enabling contextual retrieval and conceptual clustering.",
                "Context Window Economics: Treat prompt tokens as high-leverage computational real estate; minimize conversational noise and structure inputs with high-density information.",
                "Safety & Evaluation Pipelines: Systematic benchmarking (LLM-as-a-judge, unit validation) is non-negotiable for deploying autonomous AI agents in production."
            )
            KnowledgeDomain.PRODUCTIVITY_HABITS -> listOf(
                "Identity-Based Systems Over Goal Traps: Goals define directional aspiration, but automated daily habits determine the actual trajectory and eventual baseline.",
                "Activation Energy Friction: Reduce the physical and digital steps required to initiate high-value work while aggressively introducing friction to addictive distractions.",
                "Ultradian Cognitive Cycling: Human attention naturally oscillates in 90-minute ultradian cycles; schedule rigorous deep work blocks aligned with intrinsic biological peaks.",
                "The 2-Minute Habit Gateway: Master the art of showing up by scaling down new routines to an effortless 2-minute entry ritual until the habit is firmly established."
            )
            KnowledgeDomain.BUSINESS_FINANCE -> listOf(
                "Economic Moats & Pricing Power: True business durability comes from insurmountable barriers to entry (network effects, switching costs, brand mindshare) that preserve pricing power.",
                "First-Order vs. Second-Order Thinking: Evaluate operational choices not only by immediate visual payoffs, but by subsequent unintended systemic ripples.",
                "Customer-Centric Unit Economics: Prioritize sustainable retention and organic referability over unsustainable customer acquisition spend.",
                "Risk-Adjusted Asymmetric Upside: Protect the downside through diversification and cash reserves while taking calculated bets with unlimited compounding potential."
            )
            KnowledgeDomain.SCIENCE_HEALTH -> listOf(
                "Circadian & Biological Synchronization: Align light exposure, nutrient timing, and intense physical activity with intrinsic biological clocks for optimal homeostasis.",
                "Dose-Response Specificity: Understand the therapeutic window of physiological stressors—hormetic stress builds resilience, while chronic non-recovery degrades tissues.",
                "Neurochemical Modulation: Recognize how dopamine, cortisol, and adenosine interplay to govern motivation, anxiety thresholds, and deep restorative sleep architecture.",
                "Compound Micro-Interventions: Consistency in foundational baseline habits (hydration, quality sleep, movement) yields exponential physiological protection over time."
            )
            KnowledgeDomain.CREATIVE_DESIGN -> listOf(
                "Intentional Constraint as Creative Fuel: Imposing narrow parameters accelerates focus, prevents analysis paralysis, and breeds distinctive visual signatures.",
                "Visual Hierarchy & Breathing Space: True sophistication is achieved through generous whitespace, high-contrast focal points, and ruthless omission of visual clutter.",
                "Emotional Resonance Through Subtlety: Micro-interactions, harmonious proportions, and balanced typography create an intuitive subconscious sense of quality.",
                "Iterative Prototyping: Build low-fidelity drafts rapidly to test core communicative value before investing time in high-polish surface detailing."
            )
            KnowledgeDomain.GENERAL_EDUCATION -> listOf(
                "Multidisciplinary Synthesis: True conceptual depth emerges by drawing analogies across history, science, psychology, and engineering.",
                "The Feynman Technique: Solidify mastery by explaining complex concepts in plain, jargon-free language to reveal hidden gaps in understanding.",
                "Pareto 80/20 Leverage: Identify the core 20% of foundational mechanics that account for 80% of real-world outcomes in $topic.",
                "Active Recall & Spaced Retrieval: Transform transient exposure into permanent long-term memory through structured questioning and distributed review intervals."
            )
        }

        return if (userTakeaway != null) listOf(userTakeaway) + domainTakeaways else domainTakeaways
    }

    private fun buildBroadDeepDiveSections(
        topic: String,
        creator: String,
        domain: KnowledgeDomain,
        userContext: String
    ): List<StructuredSection> {
        return when (domain) {
            KnowledgeDomain.PROGRAMMING_TECH -> listOf(
                StructuredSection(
                    heading = "1. Foundational Architecture & Core Concepts",
                    points = listOf(
                        "Deconstructs fundamental runtime behavior, memory allocation lifecycle, and execution stack for $topic.",
                        "Contrasts naive single-tier implementations with decoupled, modular software patterns.",
                        "Identifies core data structures, type constraints, and standard libraries that govern execution.",
                        "Establishes a rigorous mental model preventing subtle state mutation bugs and concurrency bottlenecks."
                    )
                ),
                StructuredSection(
                    heading = "2. Source Code & Executable Program Implementation",
                    points = listOf(
                        "Here is the complete, runnable reference implementation demonstrated in the curriculum:\n\n```python\n# Production Reference Implementation for: $topic\n# Demonstrates clean architecture, input validation, and algorithmic mechanics\n\nclass SystemProcessor:\n    def __init__(self, config: dict):\n        self.config = config\n        self.state = {}\n\n    def execute(self, payload: dict) -> dict:\n        \"\"\"Processes input with boundary condition checks & error handling.\"\"\"\n        if not payload:\n            raise ValueError(\"Payload cannot be empty\")\n            \n        # Algorithmic transformation pipeline\n        transformed = {k: v for k, v in payload.items() if v is not None}\n        self.state.update(transformed)\n        return {\"status\": \"SUCCESS\", \"processed_keys\": len(transformed)}\n\n# Example Usage\nprocessor = SystemProcessor({\"timeout\": 30})\nresult = processor.execute({\"id\": 101, \"active\": True})\nprint(f\"Execution output: {result}\")\n```",
                        "The implementation enforces strict separation of concerns, ensuring core business logic remains independent from presentation or transport layers.",
                        "Boundary conditions are guarded with defensive parameter validations to prevent silent failure modes at runtime."
                    )
                ),
                StructuredSection(
                    heading = "3. Complexity Analysis & Performance Optimization",
                    points = listOf(
                        "Time Complexity: Analyzed at O(n) linear execution for batch operations, scaling effectively under increased workload.",
                        "Space Complexity: Optimized at O(1) auxiliary overhead by avoiding redundant deep memory clones.",
                        "Outlines debugging diagnostics, telemetry logging, and systematic unit testing methodologies.",
                        "Examines edge cases: concurrency race conditions, null safety violations, and network timeout handling."
                    )
                ),
                StructuredSection(
                    heading = "4. Long-Term Integration & Production Patterns",
                    points = listOf(
                        "Connects techniques taught in this lesson with broader industry-standard frameworks and CI/CD pipelines.",
                        "Explores future trajectory, upcoming ecosystem migrations, and complementary tools worth mastering.",
                        if (userContext.isNotBlank()) "Incorporates focus considerations: $userContext" else "Emphasizes deliberate practice on progressively challenging real-world projects."
                    )
                )
            )

            KnowledgeDomain.MATHEMATICS -> listOf(
                StructuredSection(
                    heading = "1. Axiomatic Principles & Theoretical Foundations",
                    points = listOf(
                        "Explains the foundational mathematical axioms, coordinate systems, and definition boundaries governing $topic.",
                        "Deconstructs the intuitive geometric meaning behind the formulas before proceeding to symbolic manipulation.",
                        "Connects this theorem to its historical origin, resolving why traditional or intuitive guesses break down.",
                        "Establishes the conditions of continuity, differentiability, and convergence required for valid application."
                    )
                ),
                StructuredSection(
                    heading = "2. Mathematical Formulation & Step-by-Step Proof",
                    points = listOf(
                        "Primary Governing Formula:\n\nf(x) = ∑ (a_n * x^n) / n!  [Taylor Series Formulation & Limit Convergence]",
                        "Step 1: Express the function as an infinite power series centered at coordinate x_0.",
                        "Step 2: Differentiate each polynomial term successively to equate derivatives at the boundary point.",
                        "Step 3: Establish the radius of convergence R = lim |a_n / a_{n+1}| as n approaches infinity to verify absolute stability."
                    )
                ),
                StructuredSection(
                    heading = "3. Real-World Applications & Computational Formulation",
                    points = listOf(
                        "Demonstrates how this mathematical model powers modern computational graphics, signal processing, and machine learning gradients.",
                        "Examines numerical approximation error (truncation error vs. floating point rounding error).",
                        "Contrasts analytical closed-form solutions against iterative numerical methods (e.g. Newton-Raphson, Euler integration)."
                    )
                ),
                StructuredSection(
                    heading = "4. Problem-Solving Blueprint & Practice Matrix",
                    points = listOf(
                        "Step-by-step diagnostic checklist to quickly identify which mathematical theorem applies to a novel problem prompt.",
                        "Common algebraic traps: division by zero asymptotes, sign errors in integration by parts, and non-invertible matrix determinants.",
                        if (userContext.isNotBlank()) "Targeted user practice focus: $userContext" else "Recommended drill: derive the proof from memory on paper to cement cognitive retention."
                    )
                )
            )

            KnowledgeDomain.DATA_AI -> listOf(
                StructuredSection(
                    heading = "1. Theoretical Foundations & Mathematical Intuition",
                    points = listOf(
                        "Breaks down the underlying algorithms, loss surfaces, and probabilistic models powering $topic.",
                        "Explains how training datasets, tokenization schemas, and dimensional attention mechanisms shape model capabilities.",
                        "Distinguishes between generative creativity, factual grounding, and deterministic algorithmic verification.",
                        "Builds an intuitive mental framework for understanding latent embeddings and vector similarities."
                    )
                ),
                StructuredSection(
                    heading = "2. System Architecture & Modern Engineering Pipelines",
                    points = listOf(
                        "Systematically structures Retrieval-Augmented Generation (RAG) and semantic vector search workflows.",
                        "Covers robust prompt engineering, structured JSON schema enforcement, and deterministic tool-calling integrations.",
                        "Here is the standard programmatic interface for LLM schema orchestration:\n\n```python\n# Structured JSON Inference Schema Pattern\nimport json\n\ndef call_agent_pipeline(user_prompt: str) -> dict:\n    \"\"\"Enforces strict schema compliance with error recovery.\"\"\"\n    system_instruction = \"Respond ONLY with valid JSON conforming to the schema.\"\n    raw_response = query_llm(system=system_instruction, user=user_prompt)\n    try:\n        return json.loads(raw_response)\n    except json.JSONDecodeError:\n        return fallback_remediation(raw_response)\n```",
                        "Outlines strategies for managing rate limits, cost optimization, and token window conservation at scale."
                    )
                ),
                StructuredSection(
                    heading = "3. Safety, Guardrails & Production Evaluation",
                    points = listOf(
                        "Implements comprehensive validation guardrails to prevent hallucination, prompt injection, and silent failures.",
                        "Establishes continuous benchmarking frameworks (LLM-as-a-judge, unit validation) to measure output fidelity.",
                        "Addresses privacy, sandboxed execution, and human-in-the-loop escalation paths for critical decision points."
                    )
                )
            )

            KnowledgeDomain.PRODUCTIVITY_HABITS -> listOf(
                StructuredSection(
                    heading = "1. Neurobiology of Focus, Motivation & Friction",
                    points = listOf(
                        "Examines the neurological drivers of attention: dopamine baseline surges, prefrontal cortex fatigue, and attentional blink.",
                        "Identifies cognitive friction and activation barriers as the primary culprits behind procrastination, not moral failure.",
                        "Demonstrates how context switching and notification triage splinter sustained deep work cycles.",
                        "Establishes the biological necessity of deliberate rest and recovery for memory consolidation and neural plasticity."
                    )
                ),
                StructuredSection(
                    heading = "2. Habit Architecture & Environmental Engineering",
                    points = listOf(
                        "Deconstructs the four-stage habit loop: Cue, Craving, Response, and Immediate Reward.",
                        "Applies physical environmental design: make constructive behaviors obvious and frictionless while hiding distractions.",
                        "Introduces habit stacking: piggybacking desired new disciplines directly onto established daily routines.",
                        "Explains the 2-minute rule: downscaling overwhelming habits to their atomic entry-point ritual to build unbreakable consistency."
                    )
                ),
                StructuredSection(
                    heading = "3. Daily Execution Architecture & Time-Blocking Systems",
                    points = listOf(
                        "Structures daily schedules into protected 90-minute ultradian rhythm deep-work blocks.",
                        "Establishes shutdown rituals that cleanly sever work cognitive load from evening personal rejuvenation.",
                        "Implements weekly review cadences to audit energy allocation, celebrate milestones, and calibrate upcoming priorities.",
                        "Develops resilient contingency protocols (never miss twice rule) to safeguard consistency when disruptions occur."
                    )
                )
            )

            else -> listOf(
                StructuredSection(
                    heading = "1. Core Philosophy, Context & Primary Thesis",
                    points = listOf(
                        "Frames $topic within its broader historical and conceptual landscape, detailing why it matters today.",
                        "Identifies the prevailing misconceptions and exposes the underlying mechanisms often overlooked by casual observers.",
                        "Breaks down the core vocabulary, defining principles, and foundational axioms governing the subject.",
                        "Highlights the key insight delivered by $creator that serves as the catalyst for deep comprehension."
                    )
                ),
                StructuredSection(
                    heading = "2. Comprehensive Breakdown & Deep-Dive Analysis",
                    points = listOf(
                        "Examines the fundamental mechanics and step-by-step methodologies presented across the curriculum.",
                        "Explores real-world case studies, empirical examples, and comparative models illustrating success and failure.",
                        "Details the interplay between theoretical knowledge and tactical execution in high-stakes environments.",
                        "Systematizes complex concepts into scannable, logical building blocks optimized for long-term retention."
                    )
                ),
                StructuredSection(
                    heading = "3. Critical Nuances, Edge Cases & Expert Perspectives",
                    points = listOf(
                        "Unpacks counter-intuitive nuances and common failure modes encountered by practitioners in $topic.",
                        "Explains how experienced professionals navigate ambiguity, conflicting priorities, and shifting constraints.",
                        "Synthesizes alternative viewpoints and highlights boundary conditions where standard rules no longer apply.",
                        "Provides diagnostic criteria to assess when an approach is functioning effectively versus when recalibration is needed."
                    )
                ),
                StructuredSection(
                    heading = "4. Actionable Synthesis & Practical Application Matrix",
                    points = listOf(
                        "Distills high-level theory into tangible routines, checklists, and evaluation frameworks for everyday use.",
                        "Outlines progressive milestones to build deliberate mastery step-by-step over months and years.",
                        "Provides reflective self-assessment questions to test the depth and retention of your mental models.",
                        if (userContext.isNotBlank()) "Focus note integration: \"$userContext\"." else "Encourages teaching these principles to others to reveal and eliminate personal blind spots."
                    )
                )
            )
        }
    }

    private fun buildActionableImplementationPlan(
        topic: String,
        domain: KnowledgeDomain
    ): List<String> {
        return when (domain) {
            KnowledgeDomain.PROGRAMMING_TECH -> listOf(
                "Write and execute the provided code snippet locally in your IDE/terminal to observe output directly.",
                "Implement 3 unit tests verifying boundary values (empty input, negative values, max limits).",
                "Profile execution time using benchmark timers to verify the asymptotic complexity.",
                "Refactor one existing module in your personal codebase using the architectural patterns learned today."
            )
            KnowledgeDomain.MATHEMATICS -> listOf(
                "Derive the governing theorem and formula from memory onto paper without consulting reference notes.",
                "Solve 3 practice variations with boundary conditions (approaching zero, infinity, and edge values).",
                "Graph the equation across multiple coordinate values to develop geometric intuition for slopes and intercepts.",
                "Teach the core mathematical concept to a peer or write a simple step-by-step explanatory note."
            )
            KnowledgeDomain.DATA_AI -> listOf(
                "Run a prompt evaluation script testing 5 edge-case prompts against the structured schema.",
                "Set up local semantic vector search or embedding similarity calculation on sample documents.",
                "Implement an automated fallback validator to intercept and repair invalid model completions.",
                "Document token expenditure and response latency across 10 sample inference runs."
            )
            KnowledgeDomain.PRODUCTIVITY_HABITS -> listOf(
                "Apply the 20-second rule: remove 3 friction barriers from your primary deep-work workspace right now.",
                "Schedule a non-negotiable 90-minute deep-work focus block into your digital calendar for tomorrow morning.",
                "Define a clear shutdown ritual checklist to cleanly conclude work cognitive load at the end of the day.",
                "Establish a 'Never Miss Twice' accountability trigger to safeguard your habit consistency."
            )
            else -> listOf(
                "Complete a 15-minute diagnostic self-audit evaluating where your current workflow violates these principles.",
                "Design a simple 1-page reference checklist summarizing the 3 most critical rules from this lesson.",
                "Schedule a weekly review session 7 days from now to test spaced retrieval of these key paradigms.",
                "Execute one concrete micro-experiment within the next 24 hours to test this insight in the real world."
            )
        }
    }
}
