class_name AdAdapter
extends RefCounted
## Disabled adapter. A real implementation must gate SDK initialization on consent.
signal reward_resolved(earned: bool)
func available() -> bool:
	return false
func show_interstitial() -> bool:
	return false
func show_rewarded() -> void:
	reward_resolved.emit(false)
