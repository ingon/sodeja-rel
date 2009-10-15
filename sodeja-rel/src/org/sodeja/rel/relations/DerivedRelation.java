package org.sodeja.rel.relations;

import java.util.Set;

import org.sodeja.rel.Entity;
import org.sodeja.rel.Relation;

public abstract class DerivedRelation implements Relation {
	protected final String name;
	protected final Relation relation;
	
	public DerivedRelation(String name, Relation relation) {
		this.name = name;
		this.relation = relation;
	}

	@Override
	public Set<Entity> select() {
		throw new UnsupportedOperationException();
	}
}
