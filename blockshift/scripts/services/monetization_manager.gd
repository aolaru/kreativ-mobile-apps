class_name MonetizationManager
extends RefCounted
var purchases: PurchaseAdapter = PurchaseAdapter.new()
var ads: AdAdapter = AdAdapter.new()
var verified_pro: bool = false
var consent_granted: bool = false
var last_ad_seconds: float = -1.0

func can_show_interstitial(active_game: bool, tutorial: bool, finished_games: int, session_seconds: float) -> bool:
	if verified_pro or not consent_granted or active_game or tutorial or not ads.available():
		return false
	if finished_games < int(Rules.value("ads_min_games")) or session_seconds < int(Rules.value("ads_min_seconds")):
		return false
	return last_ad_seconds < 0 or session_seconds - last_ad_seconds >= float(Rules.value("ads_min_seconds"))

func try_interstitial(active_game: bool, tutorial: bool, finished_games: int, session_seconds: float) -> bool:
	if can_show_interstitial(active_game, tutorial, finished_games, session_seconds) and ads.show_interstitial():
		last_ad_seconds = session_seconds
		return true
	return false
