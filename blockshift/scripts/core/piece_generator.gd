class_name PieceGenerator
extends RefCounted
## Seeded weighted catalog. At most two singletons per hand. Ensure one fit at deal.
var rng: RandomNumberGenerator = RandomNumberGenerator.new()
var catalog: Array = []

func _init(seed_value: int = 1) -> void:
	rng.seed = seed_value
	catalog = JSON.parse_string(FileAccess.get_file_as_string("res://config/pieces.json"))

func make_piece(id: int) -> Dictionary:
	var entry: Dictionary = catalog[id]
	return {"id": id, "cells": entry.cells.duplicate(true), "color": id % 4 + 1}

func hand(board: Board) -> Array:
	var result: Array = []
	var singles: int = 0
	for slot in 3:
		var id: int = _weighted()
		if id == 0 and singles >= 2:
			id = 1
		singles += int(id == 0)
		result.append(make_piece(id))
	if not board.has_move(result):
		var fits: Array[int] = []
		for id in catalog.size():
			if not board.positions(catalog[id].cells).is_empty():
				fits.append(id)
		if not fits.is_empty():
			result[rng.randi_range(0, 2)] = make_piece(fits[rng.randi_range(0, fits.size() - 1)])
	return result

func _weighted() -> int:
	var total: int = 0
	for entry in catalog:
		total += int(entry.weight)
	var roll: int = rng.randi_range(1, total)
	for id in catalog.size():
		roll -= int(catalog[id].weight)
		if roll <= 0:
			return id
	return 0
