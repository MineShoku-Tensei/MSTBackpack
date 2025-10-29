package com.MineShoku.Backpack;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Pair<V, T>(V first, T second) {
	@NotNull
	@Contract(pure = true)
	public String toString() {
		return "(" + this.first + ", " + this.second + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Pair<?, ?>(Object o1, Object o2))) return false;
		return Objects.equals(this.first, o1) && Objects.equals(this.second, o2);
	}

	public int hashCode() {
		return Utils.hashCode(this.first, this.second);
	}
}