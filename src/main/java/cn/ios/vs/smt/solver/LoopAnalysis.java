
package cn.ios.vs.smt.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

/**
 * The summary of doInBackground method of AsyncTask
 * @author Baoquan Cui
 * @version 1.0
 */
public class LoopAnalysis {

	private List<UnitInfo> mUnitInfos = new ArrayList<UnitInfo>();

	private Set<Unit> mLoopHeaderList = new HashSet<>();

	private UnitGraph mUnitGraph = null;

	private SootMethod methodUnderAnalysis = null;
	
	public LoopAnalysis(SootMethod analysisMethod) {
		this.methodUnderAnalysis = analysisMethod;
		mUnitGraph = new BriefUnitGraph(this.methodUnderAnalysis.getActiveBody());
	}

	public Set<Unit> getLoopStartUnits() {
		return mLoopHeaderList;
	}

	public SootMethod getMethodUnderAnalysis() {
		return methodUnderAnalysis;
	}
	
	public List<UnitInfo> getmUnitInfos() {
		return mUnitInfos;
	}

	public void setMethodUnderAnalysis(SootMethod methodUnderAnalysis) {
		this.methodUnderAnalysis = methodUnderAnalysis;
	}


	public void generation(Set<SootMethod> visitedMethod) {
		traverUnitGraph(mUnitGraph.getHeads().get(0), 0);

		for (UnitInfo unitInfo : mUnitInfos) {
			if (unitInfo.mLoopHeaderUnit == null) {
				continue;
			}
			mLoopHeaderList.add(unitInfo.mLoopHeaderUnit);
		}
		mLoopHeaderList.remove(null);
	}

	private Unit traverUnitGraph(Unit b0, int deepFirstSearchPathPosition) {
		// return: innermost loop header of b0
		UnitInfo unitInfo = getUnitInfo(b0);
		unitInfo.visited = true;
		unitInfo.mDeepFirstSearchPathPosition = deepFirstSearchPathPosition;
		for (Unit b : mUnitGraph.getSuccsOf(b0)) {
			UnitInfo unitInfob = getUnitInfo(b);
			if (!unitInfob.visited) {
				// case(A)
				Unit nh = traverUnitGraph(b, deepFirstSearchPathPosition + 1);
				tagLoopHeader(b0, nh);
			} else {
				if (unitInfob.mDeepFirstSearchPathPosition > 0) {
					// case(B)
					unitInfob.visited = true;
					tagLoopHeader(b0, b);
				} else if (unitInfob.mLoopHeaderUnit == null) {
					// case(C)

				} else {
					Unit h = unitInfob.mLoopHeaderUnit;
					UnitInfo unitInfoH = getUnitInfo(h);
					if (unitInfoH.mDeepFirstSearchPathPosition > 0) {
						// case(D)
						tagLoopHeader(b0, h);
					} else {
						// case(E) re-entry
						while (unitInfoH.mLoopHeaderUnit != null) {
							unitInfoH = getUnitInfo(unitInfoH.mLoopHeaderUnit);
							if (unitInfoH.mDeepFirstSearchPathPosition > 0) {
								tagLoopHeader(b0, h);
								break;
							}
						}
					}
				}
			}
		}
		unitInfo.mDeepFirstSearchPathPosition = 0;
		return unitInfo.mLoopHeaderUnit;
	}

	private void tagLoopHeader(Unit b, Unit h) {
		if (b == h || h == null) {
			return;
		}
		UnitInfo cur1 = getUnitInfo(b);
		UnitInfo cur2 = getUnitInfo(h);
		while (cur1.mLoopHeaderUnit != null) {
			UnitInfo ih = getUnitInfo(cur1.mLoopHeaderUnit);
			if (ih == cur2) {
				return;
			}
			if (ih.mDeepFirstSearchPathPosition < cur2.mDeepFirstSearchPathPosition) {
				cur1.mLoopHeaderUnit = cur2.mUnit;
				cur1 = cur2;
				cur2 = ih;
			} else {
				cur1 = ih;
			}
		}
		cur1.mLoopHeaderUnit = cur2.mUnit;

	}

	public UnitInfo getUnitInfo(Unit unit) {
		for (UnitInfo unitInfo : mUnitInfos) {
			if (unitInfo.mUnit.equals(unit)) {
				return unitInfo;
			}
		}
		UnitInfo unitInfo = new UnitInfo();
		unitInfo.mUnit = unit;
		mUnitInfos.add(unitInfo);
		return unitInfo;
	}
	
	public Unit getLoopHeader(Unit unit) {
		return getUnitInfo(unit).mLoopHeaderUnit;
	}
	
	/**
	 *  
	 */
	static class UnitInfo {
		private Unit mUnit = null;
		private boolean visited = false;
		private int mDeepFirstSearchPathPosition = 0;
		private Unit mLoopHeaderUnit = null;
		
		public Unit getLoopHeader() {
			return mLoopHeaderUnit;
		}

		@Override
		public boolean equals(Object obj) {
			boolean result = mUnit
					.equals(obj instanceof UnitInfo ? ((UnitInfo) obj).mUnit
							: obj);
			return result;
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("\n******************\n");
			sb.append("unit-->" + mUnit);
			sb.append("\n");
			sb.append("iloop_header-->" + mLoopHeaderUnit);
			sb.append("\n");
			sb.append("DFEP_pos-->" + mDeepFirstSearchPathPosition);
			return sb.toString();
		}

	}

}
