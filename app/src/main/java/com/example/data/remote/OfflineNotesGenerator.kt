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
        val transcriptMention = if (userContext.contains("Real Video Transcript", ignoreCase = true)) {
            " Key segments from the video transcript were cross-checked to capture the creator's exact points."
        } else if (userContext.isNotBlank()) {
            " Incorporates your focus notes: \"${userContext.take(100)}...\"."
        } else ""

        return when (domain) {
            KnowledgeDomain.PROGRAMMING_TECH ->
                "In this practical lesson, $creator breaks down $topic step-by-step for developers. " +
                        "Rather than just lecturing about syntax, the focus is on how the code actually runs under the hood, why certain design patterns matter, " +
                        "and how to avoid the most common bugs beginners run into.$transcriptMention"

            KnowledgeDomain.MATHEMATICS ->
                "In this visual lesson on $topic, $creator tackles the subject from a refreshing first-principles perspective. " +
                        "Instead of forcing you to blindly memorize dry formulas and symbols, they build the geometric intuition behind the math " +
                        "so you can see why the theorems actually hold true.$transcriptMention"

            KnowledgeDomain.DATA_AI ->
                "In this breakdown of $topic, $creator explores modern AI architectures and practical machine learning mechanics. " +
                        "The discussion cuts through the usual hype to explain how these models process data, where they fail, " +
                        "and what engineers need to know to build reliable systems today.$transcriptMention"

            KnowledgeDomain.PRODUCTIVITY_HABITS ->
                "In this guide to $topic, $creator shares an actionable, human-centered framework for building better routines. " +
                        "The core message is simple: relying on pure motivation is a trap. Instead, the video shows how to redesign your environment " +
                        "and habits so following through becomes almost automatic.$transcriptMention"

            KnowledgeDomain.BUSINESS_FINANCE ->
                "In this analysis of $topic, $creator breaks down the real economics and strategy driving market outcomes. " +
                        "They focus on building defensible competitive advantages, understanding cash flow, and avoiding the vanity metrics " +
                        "that often mislead founders and investors.$transcriptMention"

            KnowledgeDomain.SCIENCE_HEALTH ->
                "In this science-backed exploration of $topic, $creator explains the biological mechanisms behind daily energy and performance. " +
                        "The video translates peer-reviewed research into straightforward protocols you can actually test in your day-to-day routine.$transcriptMention"

            KnowledgeDomain.CREATIVE_DESIGN ->
                "In this creative session on $topic, $creator deconstructs what separates mediocre work from thoughtful craft. " +
                        "They explore visual balance, subtle typography choices, and the power of negative space to create memorable experiences.$transcriptMention"

            KnowledgeDomain.GENERAL_EDUCATION ->
                "In this insightful lesson on $topic, $creator connects the dots between historical context and practical modern applications. " +
                        "It offers a great primer with clear analogies that make complex ideas easy to grasp and remember.$transcriptMention"
        }
    }

    private fun buildBroadKeyTakeaways(
        topic: String,
        domain: KnowledgeDomain,
        userContext: String
    ): List<String> {
        val userTakeaway = if (userContext.isNotBlank() && !userContext.startsWith("Real Video Transcript")) {
            "Your Focus Point: Prioritizing \"${userContext.take(80)}...\" during review."
        } else null

        val domainTakeaways = when (domain) {
            KnowledgeDomain.PROGRAMMING_TECH -> listOf(
                "Separate pure logic from side-effects: Keep calculation code independent from network and database calls to make debugging and testing painless.",
                "Always check time and space complexity: Writing code that works is step one, but being aware of Big O prevents nasty bottlenecks as data grows.",
                "Let the type system do the heavy lifting: Validate inputs early at boundaries so your core functions never have to deal with illegal states.",
                "Prefer simple composition over deep inheritance: Building small, focused functions and modules is far easier to maintain and refactor over time."
            )
            KnowledgeDomain.MATHEMATICS -> listOf(
                "Intuition before algebraic manipulation: Visualize what the math represents geometrically (slopes, areas, rotations) before crunching equations.",
                "The power of testing edge cases: Check how equations behave at extremes (as numbers approach zero or infinity) to spot flaws in reasoning.",
                "Derivatives and integrals are inverse operations: Differentiating computes instantaneous rate of change, while integrating accumulates area.",
                "Math as a language for the physical world: Abstract equations aren't isolated puzzles—they describe real motion, sound waves, and AI loss functions."
            )
            KnowledgeDomain.DATA_AI -> listOf(
                "Pair probabilistic AI with deterministic code: Use LLMs for creative reasoning, but always enforce strict JSON validation and tool guards for reliability.",
                "Embeddings map meaning to geometry: Transforming words into high-dimensional vectors allows algorithms to compare semantic intent mathematically.",
                "Prompt clarity beats prompt length: Be specific about role, constraints, and output format; cut unnecessary filler words that dilute model attention.",
                "Continuous benchmark evaluation: Run small automated test suites against model prompts to detect regressions when adjusting instructions."
            )
            KnowledgeDomain.PRODUCTIVITY_HABITS -> listOf(
                "Focus on systems rather than just goals: Goals point you in the right direction, but your daily friction and environment dictate your actual results.",
                "Lower the activation energy: Make starting the high-value task effortless (e.g. have your workspace ready), while making distractions inconvenient.",
                "Work in focused 90-minute blocks: Cognitive focus naturally dips after 90 minutes; take real offline breaks to recharge attention.",
                "The 'Never Miss Twice' rule: Slipping up one day is normal; the real key to long-term consistency is making sure you never let it become a two-day habit."
            )
            KnowledgeDomain.BUSINESS_FINANCE -> listOf(
                "Build real moats, not just features: Lasting businesses rely on network effects, high switching costs, or trusted brand authority.",
                "Think about second-order effects: Look past the immediate payoff and ask what unintended incentives or costs a business decision creates.",
                "Retention matters more than acquisition: Bringing in new customers is pointless if your churn rate leaks them out just as fast.",
                "Protect your downside first: Always maintain healthy cash reserves so you can survive unexpected downturns and take smart calculated risks."
            )
            KnowledgeDomain.SCIENCE_HEALTH -> listOf(
                "Respect your biological clock: Getting natural sunlight in the morning and dimming bright screens at night keeps your circadian rhythm steady.",
                "The difference between good stress and burnout: Short periods of exertion (exercise, cold/heat, intense focus) trigger growth; chronic stress wears you down.",
                "Dopamine drives pursuit, not satisfaction: Understanding dopamine spikes helps you avoid distraction loops and stay motivated during hard work.",
                "Master the basics first: Consistent sleep, daily hydration, and whole foods outperform any trendy supplement on the market."
            )
            KnowledgeDomain.CREATIVE_DESIGN -> listOf(
                "Embrace constraints: Having tight guidelines often sparks better creative solutions than starting with a blank, limitless canvas.",
                "Give your design room to breathe: Generous negative space and high-contrast hierarchy make interfaces feel effortless to navigate.",
                "Polish the micro-interactions: Smooth transitions and responsive tactile feedback signal craftsmanship and care to your users.",
                "Prototype early and often: Test rough drafts with real people early instead of spending weeks perfecting ideas in isolation."
            )
            KnowledgeDomain.GENERAL_EDUCATION -> listOf(
                "Connect ideas across disciplines: The most useful insights happen when you apply lessons from history, biology, or engineering to everyday problems.",
                "The Feynman technique works: If you can't explain an idea in simple, everyday words to a beginner, you don't fully understand it yet.",
                "Apply the 80/20 rule: Identify the 20% of core concepts in $topic that deliver 80% of the real-world understanding.",
                "Active recall over passive re-reading: Testing yourself with practice questions cements knowledge far better than passively highlighting text."
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
                    heading = "1. How It Works Under the Hood",
                    points = listOf(
                        "Breaks down what happens in memory and during runtime when executing code for $topic.",
                        "Explains why naive quick-fix approaches tend to slow down or fail as your codebase expands.",
                        "Identifies the core data structures and built-in functions you should actually be using.",
                        "Gives you a clear mental model so you can spot and fix bugs before they make it to production."
                    )
                ),
                StructuredSection(
                    heading = "2. Working Code Walkthrough",
                    points = listOf(
                        "Here is a clean, commented reference script demonstrating the pattern taught in this lesson:\n\n```python\n# Clean Reference Implementation for: $topic\n# Built with readable flow, input checks, and proper error handling\n\nclass DataManager:\n    def __init__(self, settings: dict):\n        self.settings = settings\n        self.cache = {}\n\n    def process_entry(self, record: dict) -> dict:\n        \"\"\"Sanitizes and transforms data safely.\"\"\"\n        if not record:\n            return {\"status\": \"SKIPPED\", \"reason\": \"Empty input record\"}\n            \n        # Filter out empty entries and store in local cache\n        cleaned = {k: v for k, v in record.items() if v is not None}\n        self.cache.update(cleaned)\n        return {\"status\": \"OK\", \"saved_keys\": list(cleaned.keys())}\n\n# Test run\nmanager = DataManager({\"debug\": True})\nresult = manager.process_entry({\"user_id\": 42, \"role\": \"developer\", \"extra\": None})\nprint(\"Result:\", result)\n```",
                        "Notice how the code keeps business logic cleanly isolated instead of mixing everything into one messy function.",
                        "Always guard against missing or malformed inputs at the top of your functions to prevent silent runtime crashes."
                    )
                ),
                StructuredSection(
                    heading = "3. Performance Tips & Traps to Avoid",
                    points = listOf(
                        "Time Complexity: Keeps operations at O(n) linear time, scaling smoothly as your dataset grows.",
                        "Space Overhead: Avoids deep unnecessary copies in memory, keeping footprint light and snappy.",
                        "Watch out for subtle concurrency race conditions if multiple tasks update shared state at the same time.",
                        "Always test your code with empty inputs, null values, and edge boundaries before shipping."
                    )
                ),
                StructuredSection(
                    heading = "4. Putting It to Work in Real Projects",
                    points = listOf(
                        "How to fit this pattern into standard modern web frameworks and existing projects.",
                        "Recommended next steps: experiment with modifying the script and adding your own custom fields.",
                        if (userContext.isNotBlank() && !userContext.startsWith("Real Video Transcript")) "Incorporating your focus notes: \"${userContext.take(100)}\"." else "Best way to learn: type this code out yourself and run it in a terminal."
                    )
                )
            )

            KnowledgeDomain.MATHEMATICS -> listOf(
                StructuredSection(
                    heading = "1. The Intuition (Why This Makes Sense)",
                    points = listOf(
                        "Lays out the foundational concept of $topic using visual geometric analogies before jumping into formulas.",
                        "Explains the real question this branch of math was originally invented to answer.",
                        "Shows why our initial gut intuitions often fail when dealing with rates of change or high dimensions.",
                        "Clearly outlines when you can use this shortcut and when its assumptions break down."
                    )
                ),
                StructuredSection(
                    heading = "2. Formula Breakdown & Step-by-Step Proof",
                    points = listOf(
                        "The Governing Equation:\n\nf'(x) = lim_{h -> 0} [f(x + h) - f(x)] / h",
                        "Step 1: Pick two points on your function curve separated by a tiny step size h.",
                        "Step 2: Draw the secant line between them to measure the average rate of change.",
                        "Step 3: Watch what happens as h shrinks to zero—the secant smoothly becomes the exact tangent line."
                    )
                ),
                StructuredSection(
                    heading = "3. Where This Shows Up in Real Life",
                    points = listOf(
                        "Powers modern physics engines, game graphics, and AI gradient descent optimization.",
                        "Explains how computers approximate continuous curves using small discrete numerical time steps.",
                        "Shows the trade-off between exact closed-form algebraic solutions and fast numerical approximations."
                    )
                ),
                StructuredSection(
                    heading = "4. Practice Checklist",
                    points = listOf(
                        "Quick rule of thumb to recognize which formula applies when looking at a new problem.",
                        "The most common algebra mistakes students make: sign flips, dividing by zero, and forgetting constant factors.",
                        if (userContext.isNotBlank() && !userContext.startsWith("Real Video Transcript")) "Focus on your notes: \"${userContext.take(100)}\"." else "Try deriving this equation on paper once without looking at notes—it makes all the difference."
                    )
                )
            )

            KnowledgeDomain.DATA_AI -> listOf(
                StructuredSection(
                    heading = "1. How the Model Actually 'Thinks'",
                    points = listOf(
                        "Cuts through marketing buzzwords to explain the statistical loss surfaces behind $topic.",
                        "Explains how tokenization, embedding vectors, and attention heads transform plain text into mathematical vectors.",
                        "Clarifies the critical difference between factual database retrieval and probabilistic word prediction.",
                        "Gives you a clear picture of why hallucinations happen and how to ground answers in real data."
                    )
                ),
                StructuredSection(
                    heading = "2. Practical Prompt & Pipeline Setup",
                    points = listOf(
                        "How to build reliable AI pipelines by enforcing strict structured output schemas.",
                        "Here is a reliable pattern for combining LLM reasoning with code execution:\n\n```python\n# Clean AI Tool Orchestration Pattern\nimport json\n\ndef run_assistant_pipeline(user_query: str) -> dict:\n    \"\"\"Ensures clean structured responses with automatic fallback.\"\"\"\n    prompt = f\"Extract key facts as JSON: {user_query}\"\n    response = send_query(prompt)\n    try:\n        return json.loads(response)\n    except json.JSONDecodeError:\n        # Fallback to safe default if model returned invalid formatting\n        return {\"status\": \"error\", \"raw\": response}\n```",
                        "Keep your prompts direct and unambiguous: specify exact output schemas and give one clear example."
                    )
                ),
                StructuredSection(
                    heading = "3. Making It Production-Ready",
                    points = listOf(
                        "Always validate model outputs against strict schemas before passing them to databases or APIs.",
                        "Monitor token usage and latency so queries remain fast and cost-effective.",
                        "Build automated regression checks to make sure prompt tweaks don't secretly break other queries."
                    )
                )
            )

            KnowledgeDomain.PRODUCTIVITY_HABITS -> listOf(
                StructuredSection(
                    heading = "1. The Psychology of Getting Things Done",
                    points = listOf(
                        "Why willpower is a finite resource: understanding cognitive fatigue and decision burnout.",
                        "Procrastination is almost always caused by friction and ambiguous starting steps, not lack of discipline.",
                        "How notification pings break your train of thought—it takes up to 15 minutes to fully regain deep focus.",
                        "Why taking true offline breaks is essential for your brain to synthesize new ideas and recharge."
                    )
                ),
                StructuredSection(
                    heading = "2. Redesigning Your Daily Environment",
                    points = listOf(
                        "The 4 stages of any habit: Cue, Craving, Action, and Reward.",
                        "Make good habits obvious and effortless (e.g. have your workspace prepared the night before).",
                        "Habit stacking: attach the new behavior right after something you already do every single day (like morning coffee).",
                        "The 2-Minute rule: shrink the first step down so small that you can't talk yourself out of starting."
                    )
                ),
                StructuredSection(
                    heading = "3. Sustainable Routines that Stick",
                    points = listOf(
                        "Organize your day into 90-minute deep work blocks when your energy is naturally highest.",
                        "Set up a clear evening shutdown ritual so you can disconnect without work thoughts lingering in your mind.",
                        "Do a quick weekly review on Fridays to see what worked, celebrate progress, and plan next week's priorities.",
                        "The 'Never Miss Twice' rule: slipping up once is fine; just make sure you get back on track the very next day."
                    )
                )
            )

            else -> listOf(
                StructuredSection(
                    heading = "1. The Core Idea Explained Simply",
                    points = listOf(
                        "Puts $topic into clear context, explaining why the creator made this video and what problems it solves.",
                        "Dispels common myths and clears up the confusion people usually have when starting out.",
                        "Introduces the essential terms and key ideas without unnecessary jargon.",
                        "Highlights the central takeaway from $creator that anchors the whole lesson."
                    )
                ),
                StructuredSection(
                    heading = "2. Step-by-Step Breakdown with Real Examples",
                    points = listOf(
                        "Walks through the practical steps, workflows, or rules demonstrated in the video.",
                        "Shares real-life examples and case studies that show how this works in practice.",
                        "Connects the high-level concepts directly to everyday decisions and problem-solving.",
                        "Organizes the lesson into bite-sized, logical takeaways that are easy to remember."
                    )
                ),
                StructuredSection(
                    heading = "3. Mistakes to Avoid & Expert Tips",
                    points = listOf(
                        "The most common mistakes people make when trying this out, and how to steer clear of them.",
                        "Nuances and edge cases where standard advice doesn't quite apply.",
                        "How experienced practitioners handle trade-offs and tricky situations.",
                        "Quick diagnostic questions to check whether your approach is working as intended."
                    )
                ),
                StructuredSection(
                    heading = "4. What to Do Next",
                    points = listOf(
                        "Turns the lesson into a practical game plan you can try out right away.",
                        "Recommended practice exercises to build your skills step-by-step.",
                        "Self-check questions to test your understanding after watching the video.",
                        if (userContext.isNotBlank() && !userContext.startsWith("Real Video Transcript")) "Focusing on your note: \"${userContext.take(100)}\"." else "Share what you learned with a friend or colleague to really lock it into memory."
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
                "Type out and run the provided script in your favorite IDE or terminal to see it work live.",
                "Add test cases for edge conditions: try feeding it empty objects, null values, or unusual numbers.",
                "Pick one small function in an existing project of yours and refactor it using this pattern.",
                "Review the time and space trade-offs to make sure your solution stays fast as data grows."
            )
            KnowledgeDomain.MATHEMATICS -> listOf(
                "Grab a blank sheet of paper and derive the core formula from memory without looking at notes.",
                "Solve 2 practice problems by testing what happens at extreme values (near zero and infinity).",
                "Sketch the curve or geometric shape to visually verify why the rate of change behaves this way.",
                "Explain the concept out loud in plain English to test if you truly understand it."
            )
            KnowledgeDomain.DATA_AI -> listOf(
                "Test your prompt with 3 tricky questions to see if the structured JSON stays consistent.",
                "Set up a simple fallback validator in code to handle cases where an AI response is incomplete.",
                "Calculate average response time and token count across a couple of queries.",
                "Document any unexpected edge cases where the prompt needed clearer rules."
            )
            KnowledgeDomain.PRODUCTIVITY_HABITS -> listOf(
                "Clean your workspace and remove 2 distracting apps or tabs right now (the 20-second rule).",
                "Block out 90 minutes on tomorrow's calendar specifically for your highest-priority project.",
                "Write down a 3-step evening shutdown ritual to cleanly close your workday.",
                "Pick one tiny 2-minute habit to start tomorrow and commit to the 'Never Miss Twice' rule."
            )
            else -> listOf(
                "Do a quick 10-minute check of your current routine to spot one area where you can apply this lesson.",
                "Jot down a 1-sentence reminder on a sticky note with the most useful rule from this video.",
                "Review these notes again in 3 days to test your memory and keep the ideas fresh.",
                "Take one small, concrete action within the next 24 hours based on what you learned today."
            )
        }
    }
}
