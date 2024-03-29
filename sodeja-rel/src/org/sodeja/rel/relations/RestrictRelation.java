package org.sodeja.rel.relations;

import java.util.HashSet;
import java.util.Set;

import org.sodeja.collections.CollectionUtils;
import org.sodeja.collections.SetUtils;
import org.sodeja.functional.Predicate1;
import org.sodeja.rel.Condition;
import org.sodeja.rel.Entity;
import org.sodeja.rel.Relation;

public class RestrictRelation extends UnaryRelation {
	protected final Set<Condition> conditions;
	
	public RestrictRelation(String name, Relation relation, Condition... condition) {
		this(name, relation, SetUtils.asSet(condition));
	}
	
	public RestrictRelation(String name, Relation relation, Set<Condition> conditions) {
		super(name, relation);
		this.conditions = conditions;
	}

	@Override
	public Set<Entity> select() {
		Set<Entity> entities = right.select();
		return (Set<Entity>) CollectionUtils.filter(entities, new HashSet<Entity>(), new Predicate1<Entity>() {
			@Override
			public Boolean execute(Entity p) {
				// SetUtils.all
				for(Condition c : conditions) {
					if(! c.satisfied(p)) {
						return false;
					}
				}
				return true;
			}});
	}
}
