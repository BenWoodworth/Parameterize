package com.benwoodworth.parameterize.tmp

import kotlin.reflect.KProperty

// This is just a copy of your structure so I can play around without having to fix everything that breaks

public fun parameterize(
    block: ParameterizeScope.() -> Unit
) {
    // Insert your own implementation here
}

// A copy of ParametrizeScope, but as an interface
// Your existing ParametrizeScope would be an implementation of this interface

public interface ParameterizeScope {

    public fun <T> parameter(arguments: Iterable<T>): Parameter<T>

    // I removed provideDelegate because it was just used to get the name of the property,
    // which is not important for this prototype

    public operator fun <T> ParameterDelegate<T>.getValue(thisRef: Any?, property: KProperty<*>): T

    public interface Parameter<out T>

    public class ParameterDelegate<out T> internal constructor(
        internal val argument: T
    ) {

        override fun toString(): String =
            argument.toString()
    }
}

// This is an alternative implementation of the interface, that collects all parameters, without executing anything
// It could either be provided by Parameterize itself, or I could create it directly in Prepared

public class ParameterizeCollector : ParameterizeScope {
    // In this implementation, Parameter doesn't actually store the value
    // Instead, it's a unique key (using the object's identity) which is used to identify which parameter is which
    // It's also used to type-safely retrieve the correct type
    private class ParameterKey<out T> : ParameterizeScope.Parameter<T>

    private val collectedParameters = LinkedHashMap<ParameterKey<*>, Iterable<*>>()
    public val executions: Sequence<Execution>
        get() = sequence {
            // Here, insert the algo to generate all the combinations
        }

    override fun <T> parameter(arguments: Iterable<T>): ParameterizeScope.Parameter<T> {
        val key = ParameterKey<T>()
        collectedParameters += key to arguments
        return key
    }

    override fun <T> ParameterizeScope.ParameterDelegate<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
        // This function is the only thing blocking me at the moment.
        // To avoid breaking your API, it must return the actual value.
        // However, I would much rather it returns UniqueParameter, a wrapper that can later be used to retrieve the value.
        // In Prepared, that wrapper already exists: https://opensavvy.gitlab.io/prepared/api-docs/suite/opensavvy.prepared.suite/-prepared/index.html
        TODO("Not yet implemented")
    }

    public class Execution(
        public val mapping: Map<ParameterizeScope.Parameter<*>, *>,
    )
}

// This adds a very simplified Prepared-like execution environment, this would be in Prepared

public class PreparedParameterize(delegate: ParameterizeScope) : ParameterizeScope by delegate {
    public val blocks: ArrayList<PreparedParameterizeResolutionScope.() -> Unit> = ArrayList()

    public fun test(block: PreparedParameterizeResolutionScope.() -> Unit) {
        blocks += block
    }
}

public class PreparedParameterizeResolutionScope(
    private val execution: ParameterizeCollector.Execution,
) {
    public operator fun <T> ParameterizeScope.Parameter<T>.invoke(): T {
        val value = execution.mapping[this]
            ?: error("Could not access the parameter $this, maybe it was not registered yet?")

        @Suppress("UNCHECKED_CAST") // protected by Parameter's type parameter
        return value as T
    }
}

public fun preparedParameterized(block: PreparedParameterize.() -> Unit) {
    val collector = ParameterizeCollector()
    val tests = PreparedParameterize(collector).apply(block).blocks

    for (execution in collector.executions) {
        val resolutionScope = PreparedParameterizeResolutionScope(execution)
        for (test in tests) {
            test(resolutionScope)
        }
    }
}
