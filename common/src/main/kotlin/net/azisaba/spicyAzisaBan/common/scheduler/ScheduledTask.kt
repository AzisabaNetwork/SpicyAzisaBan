package net.azisaba.spicyAzisaBan.common.scheduler

abstract class ScheduledTask(val runnable: () -> Unit) {
    abstract fun cancel()
}
