package refdiff.core.diff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import refdiff.core.diff.similarity.SourceRepresentationBuilder;
import refdiff.core.io.SourceFile;
import refdiff.core.rast.HasChildrenNodes;
import refdiff.core.rast.Location;
import refdiff.core.rast.RastNode;
import refdiff.core.rast.RastNodeRelationshipType;
import refdiff.parsers.RastParser;
import refdiff.parsers.SourceTokenizer;

public class RastComparator<T> {
	
	private final RastParser parser;
	private final SourceTokenizer tokenizer;
	private final SourceRepresentationBuilder<T> srb;
	
	public RastComparator(RastParser parser, SourceTokenizer tokenizer, SourceRepresentationBuilder<T> srb) {
		this.parser = parser;
		this.tokenizer = tokenizer;
		this.srb = srb;
	}
	
	public RastDiff compare(Set<SourceFile> filesBefore, Set<SourceFile> filesAfter) throws Exception {
		return new DiffBuilder(filesBefore, filesAfter).computeDiff();
	}
	
	private class DiffBuilder {
		private RastDiff diff;
		private RastRootHelper<T> before;
		private RastRootHelper<T> after;
		private Set<RastNode> removed;
		private Set<RastNode> added;
		private final Map<RastNode, T> srMap = new HashMap<>();
		private final Map<RastNode, RastNode> mapBeforeToAfter = new HashMap<>();
		private final Map<RastNode, RastNode> mapAfterToBefore = new HashMap<>();
		private final Map<RastNode, Integer> depthMap = new HashMap<>();
		
		DiffBuilder(Set<SourceFile> filesBefore, Set<SourceFile> filesAfter) throws Exception {
			this.diff = new RastDiff(parser.parse(filesBefore), parser.parse(filesAfter));
			this.before = new RastRootHelper<T>(this.diff.getBefore());
			this.after = new RastRootHelper<T>(this.diff.getAfter());
			this.removed = new HashSet<>();
			this.diff.getBefore().forEachNode((node, depth) -> {
				this.removed.add(node);
				computeSourceRepresentation(filesBefore, node);
				depthMap.put(node, depth);
			});
			this.added = new HashSet<>();
			this.diff.getAfter().forEachNode((node, depth) -> {
				this.added.add(node);
				computeSourceRepresentation(filesAfter, node);
				depthMap.put(node, depth);
			});
		}
		
		private void computeSourceRepresentation(Set<SourceFile> filesBefore, RastNode node) {
			try {
				Location location = node.getLocation();
				for (SourceFile file : filesBefore) {
					if (file.getPath().equals(location.getFile())) {
						String sourceCode = file.getContent().substring(location.getBegin(), location.getEnd());
						T sourceCodeRepresentation = srb.buildForNode(node, tokenizer.tokenize(sourceCode));
						srMap.put(node, sourceCodeRepresentation);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		RastDiff computeDiff() {
			matchExactChildren(diff.getBefore(), diff.getAfter());
			matchMovesOrRenames();
			matchExtract();
			return diff;
		}
		
		private void matchMovesOrRenames() {
			List<PotentialMatch> candidates = new ArrayList<>();
			for (RastNode n1 : removed) {
				for (RastNode n2 : added) {
					if (sameType(n1, n2)) {
						double score = srb.similarity(srMap.get(n1), srMap.get(n2));
						if (score > 0.5) {
							PotentialMatch candidate = new PotentialMatch(n1, n2, Math.max(depthMap.get(n1), depthMap.get(n2)), score);
							candidates.add(candidate);
						}
					}
				}
			}
			Collections.sort(candidates);
			for (PotentialMatch candidate : candidates) {
				RastNode n1 = candidate.getNodeBefore();
				RastNode n2 = candidate.getNodeAfter();
				if (!sameName(n1, n2)) {
					addMatch(new Relationship(RelationshipType.RENAME, n1, n2));
				} else {
					addMatch(new Relationship(RelationshipType.MOVE, n1, n2));
				}
			}
		}

		private void matchExtract() {
			for (RastNode n2 : added) {
				for (RastNode n1After : after.findReverseRelationships(RastNodeRelationshipType.USE, n2)) {
					Optional<RastNode> optMatchingNode = matchingNodeBefore(n1After);
					if (optMatchingNode.isPresent()) {
						RastNode n1 = optMatchingNode.get();
						if (sameType(n1, n2)) {
							T sourceN1After = srMap.get(n1After);
							T sourceN1Before = srMap.get(n1);
							T removedSource = srb.minus(sourceN1Before, sourceN1After);
							double score = srb.partialSimilarity(srMap.get(n2), removedSource);
							if (score > 0.5) {
								unmarkAdded(n2);
								diff.addRelationships(new Relationship(RelationshipType.EXTRACT, n1, n2));
							}
						}
					}
				}
			}
		}

		private void matchExactChildren(HasChildrenNodes node1, HasChildrenNodes node2) {
			List<RastNode> removedChildren = children(node1, this::removed);
			List<RastNode> addedChildren = children(node2, this::added);
			for (RastNode n1 : removedChildren) {
				for (RastNode n2 : addedChildren) {
					if (sameName(n1, n2) && sameType(n1, n2)) {
						addMatch(new Relationship(RelationshipType.SAME, n1, n2));
					}
				}
			}
		}
		
		private void addMatch(Relationship relationship) {
			RastNode nBefore = relationship.getNodeBefore();
			RastNode nAfter = relationship.getNodeAfter();
			if (removed(nBefore) && added(nAfter)) {
				diff.addRelationships(relationship);
				mapBeforeToAfter.put(nBefore, nAfter);
				mapAfterToBefore.put(nAfter, nBefore);
				unmarkRemoved(nBefore);
				unmarkAdded(nAfter);
				matchExactChildren(nBefore, nAfter);
			}
		}

		private Optional<RastNode> matchingNodeBefore(RastNode n2) {
			return Optional.ofNullable(mapAfterToBefore.get(n2));
		}

		private boolean sameName(RastNode n1, RastNode n2) {
			return !n1.getLocalName().isEmpty() && n1.getLocalName().equals(n2.getLocalName());
		}
		
		private boolean sameType(RastNode n1, RastNode n2) {
			return n1.getType().equals(n2.getType());
		}
		
		private boolean removed(RastNode n) {
			return this.removed.contains(n);
		}
		
		private boolean added(RastNode n) {
			return this.added.contains(n);
		}
		
		private boolean unmarkRemoved(RastNode n) {
			return this.removed.remove(n);
		}
		
		private boolean unmarkAdded(RastNode n) {
			return this.added.remove(n);
		}
		
		private List<RastNode> children(HasChildrenNodes nodeWithChildren, Predicate<RastNode> predicate) {
			return nodeWithChildren.getNodes().stream().filter(predicate).collect(Collectors.toList());
		}
	}
	
}
