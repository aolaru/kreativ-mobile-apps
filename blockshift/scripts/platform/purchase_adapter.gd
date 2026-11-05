class_name PurchaseAdapter
extends RefCounted
## Override for a verified Google Play Billing plugin. No client-only entitlement.
signal purchase_resolved(status: String, verified_entitlement: bool)
func available() -> bool:
	return false
func localized_price() -> String:
	return "€2.99 target price"
func purchase_pro() -> void:
	purchase_resolved.emit("unavailable", false)
func restore() -> void:
	purchase_resolved.emit("unavailable", false)
