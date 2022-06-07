package net.azisaba.spicyAzisaBan.struct

// enum name MUST match with event_id (case-insensitive)
enum class EventType {
    ADD_PUNISHMENT,
    //ADDED_PROOF,
    UPDATED_PUNISHMENT, // can also be used for removal, but does not send unpunish message
    //UPDATED_PROOF,
    REMOVED_PUNISHMENT,
    //REMOVED_PROOF,
    LOCKDOWN,
}
