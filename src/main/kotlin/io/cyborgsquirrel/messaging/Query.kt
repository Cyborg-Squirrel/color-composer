package io.cyborgsquirrel.messaging

/**
 * A read intent. Loads data and assembles a response of type [R]; never mutates state or produces
 * side-effects.
 */
interface Query<R>
