package org.sodeja.rel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.sodeja.collections.PersistentSet;

class BaseRelationInfo {
	private static enum TransactionAction {
		ADD,
		REMOVE;
	}
	
	protected static class TransactionLogItem {
		protected final TransactionAction action;
		protected final Entity e;
		
		public TransactionLogItem(TransactionAction action, Entity e) {
			this.action = action;
			this.e = e;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((action == null) ? 0 : action.hashCode());
			result = prime * result + ((e == null) ? 0 : e.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TransactionLogItem other = (TransactionLogItem) obj;
			if (action == null) {
				if (other.action != null)
					return false;
			} else if (!action.equals(other.action))
				return false;
			if (e == null) {
				if (other.e != null)
					return false;
			} else if (!e.equals(other.e))
				return false;
			return true;
		}
	}
	
	public final PersistentSet<Entity> entities;
	
	public final BaseRelationIndex pkIndex;
	public final BaseRelationIndexes fkIndexes;
	
	public final PersistentSet<BaseRelationListener> listeners;
	
//	public final Set<Entity> newSet;
//	public final Set<Entity> deleteSet;
	public final List<TransactionLogItem> txLog;
	
	public final Map<ForeignKey, Set<Entity>> starvingEntities; 
	
	public BaseRelationInfo() {
		this(new PersistentSet<Entity>(),
				new BaseRelationIndex(new TreeSet<Attribute>()), new BaseRelationIndexes(),
				new PersistentSet<BaseRelationListener>(),
//				new HashSet<Entity>(), new HashSet<Entity>(),
				new LinkedList<TransactionLogItem>(),
				new HashMap<ForeignKey, Set<Entity>>());
	}

	public BaseRelationInfo(PersistentSet<Entity> entities, 
			BaseRelationIndex pkIndex, BaseRelationIndexes fkIndexes,
			PersistentSet<BaseRelationListener> listeners,
//			Set<Entity> newSet, Set<Entity> deleteSet, 
			List<TransactionLogItem> txLog,
			Map<ForeignKey, Set<Entity>> starvedEntities) {

		this.entities = entities;
		
		this.pkIndex = pkIndex;
		this.fkIndexes = fkIndexes;
		
		this.listeners = listeners;
		
//		this.newSet = newSet;
//		this.deleteSet = deleteSet;
		this.txLog = txLog;
		
		this.starvingEntities = starvedEntities;
	}
	
	public boolean hasChanges() {
		return ! txLog.isEmpty();
//		return ! (newSet.isEmpty() && deleteSet.isEmpty());
	}
	
	protected BaseRelationInfo copyDelta(PersistentSet<Entity> entities, BaseRelationIndex pkIndex, BaseRelationIndexes fkIndexes) {
		return new BaseRelationInfo(entities, pkIndex, fkIndexes, this.listeners, 
//				this.newSet, this.deleteSet, this.starvingEntities);
				this.txLog, this.starvingEntities);
	}

	public BaseRelationInfo clearCopy() {
		return new BaseRelationInfo(entities, pkIndex, fkIndexes, this.listeners, 
//				new HashSet<Entity>(), new HashSet<Entity>(), new HashMap<ForeignKey, Set<Entity>>());
				new LinkedList<TransactionLogItem>(), new HashMap<ForeignKey, Set<Entity>>());
	}

	public BaseRelationInfo addListener(BaseRelationListener l) {
		return new BaseRelationInfo(this.entities, this.pkIndex, this.fkIndexes, this.listeners.addValue(l), 
//				this.newSet, this.deleteSet, this.starvingEntities);
				this.txLog, this.starvingEntities);
	}
	
	public BaseRelationInfo addEntity(Entity e) {
		PersistentSet<Entity> newEntities = entities.addValue(e);
		
		BaseRelationIndex newPkIndex = pkIndex.insert(e);
		BaseRelationIndexes newFkIndexes = fkIndexes.insert(e);
		
//		newSet.add(e);
		txLog.add(new TransactionLogItem(TransactionAction.ADD, e));
		
		return copyDelta(newEntities, newPkIndex, newFkIndexes);
	}
	
	public BaseRelationInfo removeEntity(Entity e) {
		PersistentSet<Entity> newEntities = entities.removeValue(e);
		
		BaseRelationIndex newPkIndex = pkIndex.delete(e);
		BaseRelationIndexes newFkIndexes = fkIndexes.delete(e);
		
//		deleteSet.add(e);
		txLog.add(new TransactionLogItem(TransactionAction.REMOVE, e));
		
		return copyDelta(newEntities, newPkIndex, newFkIndexes);
	}
	
	protected BaseRelationInfo merge(BaseRelation relation, BaseRelationInfo versionInfo) {
		PersistentSet<Entity> newEntities = entities;
		BaseRelationIndex newPkIndex = pkIndex;
		BaseRelationIndexes newFkIndexes = fkIndexes;
		
		for(TransactionLogItem item : versionInfo.txLog) {
			Entity e = item.e;
			if(item.action == TransactionAction.ADD) {
				newEntities = newEntities.addValue(e);
				
				newPkIndex = newPkIndex.insert(e);
				newFkIndexes = newFkIndexes.insert(e);
				
				for(BaseRelationListener l : listeners) {
					l.inserted(relation, e);
				}
			} else {
				newEntities = newEntities.removeValue(e);
				
				newPkIndex = newPkIndex.delete(e);
				newFkIndexes = newFkIndexes.delete(e);
				
				for(BaseRelationListener l : listeners) {
					l.deleted(relation, e);
				}
			}
		}
		
		return new BaseRelationInfo(newEntities, newPkIndex, newFkIndexes, this.listeners, 
//				this.newSet, this.deleteSet, this.starvingEntities);
				this.txLog, this.starvingEntities);
	}
}
