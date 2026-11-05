class_name DailyChallenge
extends RefCounted
## Local calendar date is locked at session creation; attempts share rules and seed.
const RULE_VERSION: int = 2

static func today() -> String:
	return Time.get_date_string_from_system(false)

static func seed_for(date: String) -> int:
	var value: int = 2166136261
	for character in ("BLOCKSHIFT/v%d/" % RULE_VERSION + date).to_utf8_buffer():
		value = ((value ^ character) * 16777619) & 0x7fffffff
	return value

static func target() -> int:
	return int(Rules.value("daily_target_lines"))
