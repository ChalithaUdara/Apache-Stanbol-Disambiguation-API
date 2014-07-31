package mpi.aida.data;

import java.io.Serializable;

import mpi.aidalight.context.MentionContext;

public class Mention implements Serializable, Comparable<Mention> {

	private static final long serialVersionUID = 3177945435296705498L;

	private String mention;

	/** Starting token offset of the mention. */
	private int startToken;

	/** Ending token offset of the mention (including this token). */
	private int endToken;

	private int startStanford;

	private int endStanford;

	private int sentenceId;

	private String groundTruthEntity = null;

	private double disambiguationConfidence;

	// Character offset
	private int charOffset, charLength;

	private Entities candidateEntities;

	private String context;

	private String previousContext;

	private String followingContext;

	public void setContext(String context, String previousContext,
			String followingContext) {
		this.context = context;
		this.previousContext = previousContext;
		this.followingContext = followingContext;
	}

	public String[] getContexts() {
		String[] res = { context, previousContext, followingContext };
		return res;
	}

	private int id = -1;

	/**
	 * Occurrence count either in the collection or in a document. Set as
	 * needed.
	 */
	private int occurrenceCount = 0;

	private String type = null;

	private MentionContext localContext;

	public MentionContext getMentionContext() {
		return localContext;
	}

	public void setMentionContext(MentionContext localContext) {
		this.localContext = localContext;
	}

	private double entropy = Double.MAX_VALUE;

	public void setEntropy(double entropy) {
		this.entropy = entropy;
	}

	public double getEntropy() {
		return entropy;
	}

	public String getType() {
		if (type == null)
			return "Not Given";
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Mention() {

	}

	public Mention(String mention, int startToken, int endToken,
			int startStanford, int endStanford, int sentenceId) {
		this.startToken = startToken;
		this.endToken = endToken;
		this.startStanford = startStanford;
		this.endStanford = endStanford;
		this.mention = mention;
		this.sentenceId = sentenceId;
	}

	public Mention(String mention, int startToken, int endToken,
			int startStanford, int endStanford, int sentenceId, String type) {
		this.startToken = startToken;
		this.endToken = endToken;
		this.startStanford = startStanford;
		this.endStanford = endStanford;
		this.mention = mention;
		this.sentenceId = sentenceId;
		this.type = type;
	}

	public String getMention() {
		return mention;
	}

	public int getStartToken() {
		return startToken;
	}

	public int getEndToken() {
		return endToken;
	}

	public int getStartStanford() {
		return startStanford;
	}

	public int getEndStanford() {
		return endStanford;
	}

	public int getSentenceId() {
		return sentenceId;
	}

	public void setSentenceId(int sentenceId) {
		this.sentenceId = sentenceId;
	}

	public void addCandidateEntity(Entity entity) {
		candidateEntities.add(entity);
	}

	public Entities getCandidateEntities() {
		return candidateEntities;
	}

	public void setCandidateEntities(Entities candidateEntities) {
		this.candidateEntities = candidateEntities;
	}

	public String toString() {
		return mention + ", From:" + startToken + "/" + startStanford + ", To:"
				+ endToken + "/" + endStanford + ", Offset: " + charOffset
				+ ", sentence: " + sentenceId + ", Length: " + charLength
				+ ", Confidence: " + disambiguationConfidence;
	}

	public void setStartToken(int start) {
		this.startToken = start;
	}

	public void setEndToken(int end) {
		this.endToken = end;
	}

	public int getCharOffset() {
		return this.charOffset;
	}

	public int getCharLength() {
		return this.charLength;
	}

	public void setCharOffset(int offset) {
		this.charOffset = offset;

	}

	public void setCharLength(int length) {
		this.charLength = length;
	}

	public void setMention(String mention) {
		this.mention = mention;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mention) {
			Mention m = (Mention) obj;

			return m.getMention().equals(getMention())
					&& this.charOffset == m.charOffset;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return mention.hashCode() + charOffset;
	}

	@Override
	public int compareTo(Mention mention) {
		return this.charOffset - mention.charOffset;
	}

	public void setGroundTruthResult(String result) {
		this.groundTruthEntity = result;
	}

	public String getGroundTruthResult() {
		return groundTruthEntity;
	}

	public void setDisambiguationConfidence(double confidence) {
		disambiguationConfidence = confidence;
	}

	public double getDisambiguationConfidence() {
		return disambiguationConfidence;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setStartStanford(int startStanford) {
		this.startStanford = startStanford;
	}

	public void setEndStanford(int endStanford) {
		this.endStanford = endStanford;
	}

	public String getIdentifiedRepresentation() {
		return mention + ":::" + charOffset;
	}

	public int getOccurrenceCount() {
		return occurrenceCount;
	}

	public void setOccurrenceCount(int occurrenceCount) {
		this.occurrenceCount = occurrenceCount;
	}

}
