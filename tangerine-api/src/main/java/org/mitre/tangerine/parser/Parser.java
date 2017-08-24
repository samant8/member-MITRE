package org.mitre.tangerine.parser;

import java.io.InputStream;

import org.mitre.tangerine.exception.AETException;

public abstract class Parser<T> {

	public abstract T parse(InputStream input) throws AETException;
}
