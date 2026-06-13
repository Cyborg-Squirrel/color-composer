package io.cyborgsquirrel.messaging

/**
 * A write intent. Carries the data needed to mutate state and produce side-effects. Every command
 * resolves to a [CommandResponse] (use [EmptyResponse] when there is no value to return), which lets
 * the bus stay free of per-command generics.
 */
interface Command
