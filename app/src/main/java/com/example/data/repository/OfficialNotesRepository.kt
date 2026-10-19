package com.example.data.repository

import com.example.data.model.OfficialNote
import com.example.data.model.OfficialNoteType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

object OfficialNotesRepository {

    private val curatedNotes = listOf(
        OfficialNote(
            id = 101,
            title = "Python 3 Official Standard Library & Language Reference",
            subject = "Computer Science / Programming",
            sourceName = "Python Software Foundation (docs.python.org)",
            sourceUrl = "https://docs.python.org/3/reference/",
            authorOrInstitution = "Guido van Rossum & Python Core Developers",
            type = OfficialNoteType.OFFICIAL_DOCS,
            description = "Official Python grammar, execution model, built-in types, and standard idioms used for university coursework and technical interviews.",
            contentMarkdown = """
                # Python Core Execution Model & Reference

                ### 1. Memory Management & Object Model
                - Everything in Python is an object: types, functions, instances, integers, and classes.
                - CPython uses reference counting complemented by a generational cyclic garbage collector.
                - Variables are labels bound to memory references, not direct memory cells:
                  `a = [1, 2, 3]; b = a` -> both reference the same list instance!

                ### 2. Time & Space Complexities of Built-in Collections
                | Type | Operation | Average Case | Worst Case |
                |---|---|---|---|
                | list | append() | O(1) amortized | O(n) reallocation |
                | list | pop(0) / insert(0) | O(n) shift | O(n) |
                | dict | key lookup `d[k]` | O(1) | O(n) hash collision |
                | set | `x in s` | O(1) | O(n) |

                ### 3. Exam & Interview Cheatsheet
                ```python
                # High-yield generator comprehension
                def fibonacci(limit: int):
                    a, b = 0, 1
                    for _ in range(limit):
                        yield a
                        a, b = b, a + b
                ```
            """.trimIndent(),
            keyTakeaways = listOf(
                "CPython handles memory via reference counting and 3-generation cyclic garbage collection.",
                "Lists are dynamic contiguous arrays; appending is O(1) amortized, but insert(0) is O(n).",
                "Dict lookups and set memberships are O(1) average case backed by open addressing hash tables.",
                "Always check mutability pitfalls: default argument values in functions evaluate once at definition time."
            ),
            tags = listOf("Python", "Programming", "Data Structures", "Official Docs")
        ),
        OfficialNote(
            id = 102,
            title = "MIT 18.01 Single Variable Calculus Handwritten Lecture Notes",
            subject = "Mathematics",
            sourceName = "MIT OpenCourseWare (ocw.mit.edu)",
            sourceUrl = "https://ocw.mit.edu/courses/18-01sc-single-variable-calculus-fall-2010/",
            authorOrInstitution = "Prof. David Jerison (MIT Department of Mathematics)",
            type = OfficialNoteType.HANDWRITTEN_LECTURE,
            description = "Authentic handwritten and transcribed lecture notes covering derivatives, mean value theorem, fundamental theorem of calculus, and series convergence.",
            contentMarkdown = """
                # MIT 18.01 Single Variable Calculus: Core Lecture Summaries

                ### Lecture 1: Limit Definition of Derivative
                Limit as h -> 0 of [f(x + h) - f(x)] / h = f'(x)
                - Geometric interpretation: Tangent slope is the limiting secant slope as interval collapses to zero.
                - Differentiability implies continuity, but continuity does NOT imply differentiability (e.g. f(x) = |x| at x = 0).

                ### Lecture 2: Fundamental Theorem of Calculus (FTC)
                - FTC Part 1: If F(x) = Integral from a to x of f(t) dt, then F'(x) = f(x).
                - FTC Part 2: Integral from a to b of f(x) dx = F(b) - F(a) where F'(x) = f(x).

                ### Exam Preparation Checklist
                1. Always check endpoints when optimizing on closed intervals [a, b].
                2. L'Hôpital's Rule requires checking that limits evaluate strictly to 0/0 or infinity / infinity.
                3. Integration by parts mnemonic: Integral of u dv = uv - Integral of v du (Choose u using LIATE: Log, Inverse trig, Algebraic, Trig, Exponential).
            """.trimIndent(),
            keyTakeaways = listOf(
                "Derivative limit formula derives tangent line from collapsing secant intervals.",
                "Differentiability requires smooth continuity; sharp corners fail the derivative test.",
                "The Fundamental Theorem of Calculus bridges differentiation and accumulation integration.",
                "Use LIATE ordering to identify u when solving integration by parts."
            ),
            tags = listOf("Calculus", "MIT", "Mathematics", "Handwritten", "Derivatives")
        ),
        OfficialNote(
            id = 103,
            title = "Stanford CS106B Abstractions & Algorithms Blueprint",
            subject = "Computer Science",
            sourceName = "Stanford Engineering Everywhere",
            sourceUrl = "https://see.stanford.edu/Course/CS106B",
            authorOrInstitution = "Prof. Julie Zelenski & Nick Parlante (Stanford University)",
            type = OfficialNoteType.ACADEMIC_CHEATSHEET,
            description = "Complete algorithmic cheatsheet for recursion, binary search trees, backtracking, graphs, and big-O asymptotic analysis.",
            contentMarkdown = """
                # CS106B: Programming Abstractions & Algorithms Cheatsheet

                ### 1. Backtracking Template Pattern
                ```
                bool solve(Configuration conf) {
                    if (isGoal(conf)) return true;
                    for (Move move : validMoves(conf)) {
                        makeMove(conf, move);
                        if (solve(conf)) return true;
                        unmakeMove(conf, move); // Backtrack step
                    }
                    return false;
                }
                ```

                ### 2. Big-O Complexity Hierarchy
                O(1) < O(log n) < O(n) < O(n log n) < O(n^2) < O(2^n) < O(n!)
                - Binary Search: O(log n)
                - MergeSort & QuickSort (average): O(n log n)
                - Graph BFS / DFS: O(V + E)
                - Shortest Path (Dijkstra with priority queue): O((V + E) log V)
            """.trimIndent(),
            keyTakeaways = listOf(
                "Backtracking requires explicit choice, explore, and unchoose steps.",
                "MergeSort provides guaranteed O(n log n) runtime with O(n) auxiliary memory.",
                "BFS explores shortest unweighted path; DFS explores all depths and detects cycles.",
                "Dijkstra requires non-negative edge weights to guarantee optimal path computation."
            ),
            tags = listOf("Algorithms", "Stanford", "Recursion", "Big-O", "Data Structures")
        ),
        OfficialNote(
            id = 104,
            title = "W3C ECMAScript & Web Standards Technical Specification",
            subject = "Web Development / JavaScript",
            sourceName = "ECMA International & W3C",
            sourceUrl = "https://tc39.es/ecma262/",
            authorOrInstitution = "ECMA TC39 Technical Committee",
            type = OfficialNoteType.STANDARD_SPEC,
            description = "Official specification detailing the JavaScript event loop, microtask queues, promises, closure scoping, and prototype chain.",
            contentMarkdown = """
                # ECMAScript Specification: Event Loop & Memory Mechanics

                ### 1. Execution Context & Scopes
                - Lexical Environment consists of an Environment Record and an outer reference.
                - Closures retain references to outer Lexical Environments even after parent function execution completes.

                ### 2. Microtasks vs Macrotasks Execution Order
                1. Execute current Synchronous Call Stack to completion.
                2. Flush all pending Microtasks (`Promise.then()`, `queueMicrotask`, `MutationObserver`).
                3. Render layout / Paint (if needed).
                4. Pull and execute next Macrotask (`setTimeout`, `setInterval`, I/O).

                ### Common Exam Question
                ```javascript
                console.log('1');
                setTimeout(() => console.log('2'), 0);
                Promise.resolve().then(() => console.log('3'));
                console.log('4');
                // Output order: 1 -> 4 -> 3 -> 2
                // (3 is microtask, executes before macrotask 2!)
                ```
            """.trimIndent(),
            keyTakeaways = listOf(
                "Event loop prioritizes the microtask queue over macrotasks like setTimeout.",
                "Closures capture references, not values, from their enclosing lexical scopes.",
                "Prototype inheritance resolves properties up the chain until reaching null.",
                "Strict equality (===) prevents implicit coercion of disparate types."
            ),
            tags = listOf("JavaScript", "W3C", "Event Loop", "Specs", "Web")
        ),
        OfficialNote(
            id = 105,
            title = "Harvard CS50 Official Memory, Pointers & C Reference",
            subject = "Systems Programming",
            sourceName = "Harvard CS50 (cs50.harvard.edu)",
            sourceUrl = "https://cs50.harvard.edu/x/2024/",
            authorOrInstitution = "Prof. David J. Malan (Harvard University)",
            type = OfficialNoteType.EXAM_BLUEPRINT,
            description = "Detailed memory model notes covering stack vs heap, pointer arithmetic, buffer overflows, and dynamic memory allocation (`malloc`, `free`).",
            contentMarkdown = """
                # Harvard CS50: Memory Layout & Pointer Guide

                ### 1. Memory Segment Architecture
                ```
                [ Text / Code Segment  ] (Read-only binary instructions)
                [ Initialized Data     ] (Globals and statics)
                [ Heap (Grows Down)    ] (malloc, calloc, realloc)
                [ ... Free Memory ...  ]
                [ Stack (Grows Up)     ] (Local function frames, return addresses)
                ```

                ### 2. Pointer Rules & Dereferencing
                - `int x = 42;` creates variable x.
                - `int *p = &x;` pointer p stores memory address of x.
                - `*p = 50;` dereferences p to mutate original memory of x.
                - Never use memory after calling `free(p)` to avoid Use-After-Free vulnerabilities.
                - Always check `if (p == NULL)` after `malloc()` to guard against heap exhaustion.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Stack frames are automatically allocated and popped upon function return.",
                "Heap allocations via malloc must be manually freed to avoid memory leaks.",
                "Pointer arithmetic is scaled by the size of the underlying data type.",
                "Array indexing a[i] is syntactically equivalent to *(a + i)."
            ),
            tags = listOf("C", "Harvard", "Pointers", "Memory", "CS50")
        )
    )

    private val _notesFlow = MutableStateFlow(curatedNotes)
    val allOfficialNotes: Flow<List<OfficialNote>> = _notesFlow.asStateFlow()

    fun searchNotes(query: String, filterSubject: String = "All"): Flow<List<OfficialNote>> {
        return _notesFlow.map { list ->
            list.filter { note ->
                val matchesSubject = filterSubject == "All" || note.subject.contains(filterSubject, ignoreCase = true)
                val matchesQuery = query.isBlank() ||
                        note.title.contains(query, ignoreCase = true) ||
                        note.description.contains(query, ignoreCase = true) ||
                        note.authorOrInstitution.contains(query, ignoreCase = true) ||
                        note.tags.any { it.contains(query, ignoreCase = true) }
                matchesSubject && matchesQuery
            }
        }
    }

    fun getNoteById(id: Long): OfficialNote? {
        return curatedNotes.find { it.id == id }
    }
}
