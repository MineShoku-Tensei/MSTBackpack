package com.MineShoku.Backpack;

public interface NotFullyImplemented {
	default  <V> V notImplemented() {
		throw new UnsupportedOperationException("Not implemented");
	}
}