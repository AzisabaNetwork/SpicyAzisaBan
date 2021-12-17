package net.azisaba.spicyAzisaBan.struct

// enum name MUST match with event_id (case-insensitive)
enum class EventType {
    ADD_PUNISHMENT,
    UPDATED_PUNISHMENT, // + removal, but does not send unpunish message
    REMOVED_PUNISHMENT,
    LOCKDOWN,
}
