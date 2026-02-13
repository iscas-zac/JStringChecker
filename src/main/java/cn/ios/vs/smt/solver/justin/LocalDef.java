package cn.ios.vs.smt.solver.justin;

import java.util.ArrayList;
import java.util.List;

import soot.Local;
import soot.Unit;
import soot.jimple.DefinitionStmt;

public class LocalDef {
	
	List<? extends Unit> path = null;
	
	public LocalDef(List<? extends Unit> path) {
		this.path = path;
	}
	
	public List<Unit> getDefsOfAt(Local v, Unit context) {
    	List<Unit> defList = new ArrayList<>();
    	int index = path.indexOf(context);
		for (int i = index-1; i >=0 ; i--) {
			if(path.get(i) instanceof DefinitionStmt) {
				DefinitionStmt definitionStmt = (DefinitionStmt) path.get(i);
				if(definitionStmt.getLeftOp() instanceof Local && definitionStmt.getLeftOp().toString().equals(v.toString())) {
					defList.add(path.get(i));
				}
			}
		}
		return defList;
	}

}
