class_name Board
extends RefCounted
## Pure board model. All mutations validate first. 0 = empty, 1..4 = color.
const SIZE: int = 8
var cells: Array[int] = []

func _init() -> void:
	cells.resize(SIZE * SIZE)
	cells.fill(0)

func can_place(shape: Array, origin: Vector2i) -> bool:
	if shape.is_empty():
		return false
	for offset in shape:
		var point: Vector2i = origin + Vector2i(int(offset[0]), int(offset[1]))
		if point.x < 0 or point.x >= SIZE or point.y < 0 or point.y >= SIZE:
			return false
		if cells[point.y * SIZE + point.x] != 0:
			return false
	return true

func place(shape: Array, origin: Vector2i, color: int) -> bool:
	if color < 1 or color > 4 or not can_place(shape, origin):
		return false
	for offset in shape:
		cells[(origin.y + int(offset[1])) * SIZE + origin.x + int(offset[0])] = color
	return true

func positions(shape: Array) -> Array[Vector2i]:
	var result: Array[Vector2i] = []
	for y in SIZE:
		for x in SIZE:
			if can_place(shape, Vector2i(x, y)):
				result.append(Vector2i(x, y))
	return result

func has_move(pieces: Array) -> bool:
	for piece in pieces:
		if piece.is_empty(): continue
		for y in SIZE:
			for x in SIZE:
				if can_place(piece.cells, Vector2i(x, y)): return true
	return false

func full_lines() -> Dictionary:
	var rows: Array[int] = []
	var columns: Array[int] = []
	for i in SIZE:
		var row_full: bool = true
		var column_full: bool = true
		for j in SIZE:
			row_full = row_full and cells[i * SIZE + j] != 0
			column_full = column_full and cells[j * SIZE + i] != 0
		if row_full:
			rows.append(i)
		if column_full:
			columns.append(i)
	return {"rows": rows, "columns": columns}

func clear_lines() -> Dictionary:
	var lines: Dictionary = full_lines()
	var cleared: Array[int] = []
	for y in SIZE:
		for x in SIZE:
			if y in lines.rows or x in lines.columns:
				cleared.append(y * SIZE + x)
	for index in cleared:
		cells[index] = 0
	return {"count": lines.rows.size() + lines.columns.size(), "cells": cleared}

func shift(axis: String, index: int, direction: int) -> bool:
	if axis not in ["row", "column"] or index < 0 or index >= SIZE or absi(direction) != 1:
		return false
	var before: Array[int] = cells.duplicate()
	var changed: bool = false
	for i in SIZE:
		var source: int = index * SIZE + i if axis == "row" else i * SIZE + index
		var dest_i: int = posmod(i + direction, SIZE)
		var dest: int = index * SIZE + dest_i if axis == "row" else dest_i * SIZE + index
		cells[dest] = before[source]
		changed = changed or cells[dest] != before[dest]
	return changed

func occupied() -> int:
	var count: int = 0
	for cell in cells:
		count += int(cell != 0)
	return count

func restore(data: Variant) -> bool:
	if not data is Array or data.size() != SIZE * SIZE:
		return false
	var validated: Array[int] = []
	for cell in data:
		if not (cell is int or cell is float) or int(cell) != cell or cell < 0 or cell > 4:
			return false
		validated.append(int(cell))
	cells = validated
	return true
