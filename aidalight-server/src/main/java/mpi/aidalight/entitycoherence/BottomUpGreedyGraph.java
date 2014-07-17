package mpi.aidalight.entitycoherence;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import mpi.aida.data.Entity;
import mpi.aida.data.Mention;
import mpi.aidalight.DataStore;
import mpi.aidalight.Function;
import mpi.aidalight.MentionEntityMapping;
import mpi.aidalight.Settings;
import mpi.aidalight.kbs.EntityEntityCooccurrence;
import mpi.util.Utils;

/***
 * 
 * @author datnb
 * sharedWikitypes help to rank entities better, especially with long-tail entities.
 * For example: in domain football, if we did find some entities about Scottish football, 
 * we might rank Scottish footballer higher.
 */
public class BottomUpGreedyGraph {
  
	private Map<Mention, List<MentionEntityMapping>> mentionMappings;
	
	private TLongDoubleHashMap coh;
	
	private long MAX = 4000000l; // in order of <small id, big id>
	
	private double threshold = -2.0;
	
	private double scale_constant;
	
	private boolean final_round = false;
	
	private GraphSettings graphSettings;
	
	private Settings settings;
	
	private Map<Mention, Entity> disambiguatedMentionEntity;
	
	public static boolean testPreRecall = false;
	
	
	public BottomUpGreedyGraph(Map<Mention, List<MentionEntityMapping>> mentionMappings, 
	    Map<Mention, Entity> disambiguatedMentionEntity, Settings settings, 
	    GraphSettings graphSettings){
		this.mentionMappings = mentionMappings;
		this.disambiguatedMentionEntity = disambiguatedMentionEntity;
		this.settings = settings;
		this.graphSettings = graphSettings;
		this.final_round = false;
		
		init();
	}
	
	public BottomUpGreedyGraph(Map<Mention, List<MentionEntityMapping>> mentionMappings, 
	    Map<Mention, Entity> disambiguatedMentionEntity, Settings settings, 
	    GraphSettings graphSettings, boolean final_round){
    this.disambiguatedMentionEntity = disambiguatedMentionEntity;
    this.settings = settings;
    this.graphSettings = graphSettings;
    this.final_round = final_round;
    if(final_round) 
      this.mentionMappings = getTopKCandidates(mentionMappings);
    
    init();
  }
	
	
	
	private Map<Mention, List<MentionEntityMapping>> getTopKCandidates(Map<Mention, List<MentionEntityMapping>> mentionMappings) {
	  Map<Mention, List<MentionEntityMapping>> mentionKMappings = new HashMap<Mention, List<MentionEntityMapping>>();
	  double max_local_sim = -1.0;
	  
	  TIntIntHashMap relatedEntities = new TIntIntHashMap();
	  int N = 0; // number of disambiguated entities.
	  for(Mention mention: disambiguatedMentionEntity.keySet()) {
	    if(graphSettings.isPreprocessingLocationByLocalsimilarity() && mention.getType().equalsIgnoreCase("LOCATION"))
	      continue;
	    N++;
      Entity disambiguatedEntity = disambiguatedMentionEntity.get(mention);
      for(int entity: DataStore.getRelatedEntities(disambiguatedEntity.getId()).toArray()) {
        if(relatedEntities.contains(entity) == false)
          relatedEntities.put(entity, 1);
        else
          relatedEntities.put(entity, relatedEntities.get(entity) + 1);
      }
	  }
	  
	  int candidate_limitation = graphSettings.getNumberCandidatesPerMention();
	  for(Mention mention: mentionMappings.keySet()) {
	    if(disambiguatedMentionEntity.containsKey(mention) == false) {
	      List<MentionEntityMapping> entityMappings = mentionMappings.get(mention);
	      double max[] = new double[candidate_limitation];
	      for(int i = 0; i < candidate_limitation; i ++)
	        max[i] = -1.0;
	      MentionEntityMapping[] bestMappings = new MentionEntityMapping[candidate_limitation];
	      TIntDoubleHashMap priors = DataStore.getEntityPriors(mention.getMention());
	      for(MentionEntityMapping mapping: entityMappings) {
	        double local_sim = mapping.getLocalSimilarity();

	        Entity entity = mapping.getEntity();
	        if(relatedEntities.containsKey(entity.getId())) {
	          int val = relatedEntities.get(entity.getId());
	          if(NED.updateLocalSimByChosenEntities)
	            local_sim = Math.min(1.0, local_sim + (double)val/N);
	        }
	        mapping.setLocalSimilarity(local_sim);
	        
	        if(max_local_sim < local_sim)
	          max_local_sim = local_sim;
	        
	        for(int i = 0; i < candidate_limitation; i++) {
            if(max[i] < local_sim || 
                (max[i] == local_sim && priors.get(bestMappings[i].getEntity().getId()) < priors.get(entity.getId()))){
              // update
              for(int j = candidate_limitation - 1; j > i; j--){
                max[j] = max[j-1];
                bestMappings[j] = bestMappings[j-1];
              }
              max[i] = local_sim;
              bestMappings[i] = mapping;
              break;
            }
          }
	        
	      }
	      
	      List<MentionEntityMapping> tmp = new ArrayList<MentionEntityMapping>();
        for(int i = 0; i < candidate_limitation; i++){
          if(bestMappings[i] != null) {
            tmp.add(bestMappings[i]);
          }
          else
            break;
        }
        
        mentionKMappings.put(mention, tmp);
        
        
	    }
	    
	  }
	  scale_constant = 1.0 / max_local_sim;
	  
	  return mentionKMappings;
	}
	
	private long concatenate(long a, long b) {
	  if(a > b)
	    concatenate(b, a);
	  return a * MAX + b;
	}
	
	
	/**
	 * Calculate entity-entity coherence.
	 * First, remove noise
	 */
	private void init() {
	  //update mentionMappings map
	  Map<Mention, List<MentionEntityMapping>> tmp = new HashMap<Mention, List<MentionEntityMapping>>();
	  for(Mention mention: mentionMappings.keySet()) {
	    if(disambiguatedMentionEntity.containsKey(mention)) {
	      List<MentionEntityMapping> bestMapping = new ArrayList<MentionEntityMapping>(1);
	      for(MentionEntityMapping mapping: mentionMappings.get(mention)) {
	        if(mapping.getEntity().getId() == disambiguatedMentionEntity.get(mention).getId()) {
	          // found
	          bestMapping.add(mapping);
	          break;
	        }
	      }
	      tmp.put(mention, bestMapping);
	    }
	    else
	      tmp.put(mention, mentionMappings.get(mention));
	  }
	  
	  mentionMappings = tmp;
	  
	  coh = new TLongDoubleHashMap();
	  for(Mention mention: mentionMappings.keySet()) {
	    for(MentionEntityMapping mappingSrc: mentionMappings.get(mention)) {
	      Entity entity = mappingSrc.getEntity();
	      for(Mention m: mentionMappings.keySet()) {
	        if(mention.equals(m) == false) {
	          for(MentionEntityMapping mappingDst: mentionMappings.get(m)) {
	            Entity e = mappingDst.getEntity();
	            long key = concatenate(entity.getId(), e.getId());
	            if(entity.getId() == e.getId()) {
	              double d;
                      // should set the threshold = 0. This is just to make sure next commands never happen
	              if(Utils.getJaccardSimilarityAtTokenLevel(e.getMention().getMention(), entity.getMention().getMention()) < 0
	                  && e.getMention().getType().equalsIgnoreCase(entity.getMention().getType())){
	                
	                double sim1 = mappingSrc.getLocalSimilarity();
	                double sim2 = mappingDst.getLocalSimilarity();
	                
	                if(sim1 > sim2) 
	                  e.setWaitforRemoved(true);
	                else 
	                  entity.setWaitforRemoved(true);
	                d = 0.0;
	              }
	              else
	                d = 1.0;
	              
//	              if(graphSettings.isDecreasingRelatednessByDistance())
	              if(NED.decreaseRelatednessbyMentionDistance)
	                d /= (Math.abs(mention.getSentenceId() - m.getSentenceId()) + 1);
//	                d /= Math.log(Math.max(Math.E, Math.abs(entity.getMention().getCharOffset() - e.getMention().getCharOffset()) / BNED.BLOCK_SIZE));
	              coh.put(key, d);
	            }
	            else {
	              if(coh.containsKey(key))
	                continue;
	              double d = Function.getEntityEntityRelatedness(entity, e, settings, null);
	              if(NED.expandRelatedNessConstantbyGraph && DataStore.isRelated(entity.getId(), e.getId())) 
                  d = Math.max(1.0, settings.getExpandRelatednessConstant() * d);
	              
//	              if(graphSettings.isDecreasingRelatednessByDistance())
	              if(NED.decreaseRelatednessbyMentionDistance)
                  d /= (Math.abs(mention.getSentenceId() - m.getSentenceId()) + 1);
//	                d /= Math.log(Math.max(Math.E, Math.abs(entity.getMention().getCharOffset() - e.getMention().getCharOffset()) / BNED.BLOCK_SIZE));
	              coh.put(key, d);
	            }
	          }
	        }
	      }
	    }
	  }
	  
	}
	
	
	public Map<Mention, Entity> disambiguate(){
	  Map<Mention, Entity> results = new HashMap<Mention, Entity>();
		// find entity set that can be removed.
		PriorityQueue<EntityNode> candidates = new PriorityQueue<EntityNode>();
		TObjectIntHashMap<Mention> degree = new TObjectIntHashMap<Mention>();
		
//		Map<Mention, TIntDoubleHashMap> mention2priors = new HashMap<Mention, TIntDoubleHashMap>();
		
		double max_relatedness = 0.0;
		for(Mention mention: disambiguatedMentionEntity.keySet()){
		  results.put(mention, disambiguatedMentionEntity.get(mention));
		}
		
		for(Mention mention: mentionMappings.keySet()) {
		  if(results.containsKey(mention))
		    continue;
		  
		  List<MentionEntityMapping> mappings = mentionMappings.get(mention);
		  TIntDoubleHashMap priors = DataStore.getEntityPriors(mention.getMention());
		  // update priors, including information of disambiguated entities.
		  TIntDoubleHashMap tempPriors = new TIntDoubleHashMap();
		  double sum = 0.0; // to normalize priors.
		  for(MentionEntityMapping mapping: mappings) {
		    Entity entity = mapping.getEntity();
		    int entityId = entity.getId();
		    double prior = priors.get(entityId);
		    for(Entity givenEntity: disambiguatedMentionEntity.values())
		      prior *= EntityEntityCooccurrence.getProbability(entityId, givenEntity.getId());
		    sum += prior;
		    tempPriors.put(entityId, prior);
		  }
		  
		  for(int key: tempPriors.keys()) {
		    tempPriors.put(key, tempPriors.get(key) / sum);
		  }
		  
			int counter = 0;
			for(MentionEntityMapping mapping: mappings){
			  Entity entity = mapping.getEntity();
			  if(entity.getWaitforRemoved()){
			    counter++;
			    continue; // should be removed, don't put into the graph.
			  }
			  
        // update local similarity
			  double prior = priors.get(entity.getId());
			  double localSimilarity = mapping.getLocalSimilarity() + settings.getPriorContribution() * (tempPriors.get(entity.getId()) - prior);
        if(final_round)
          localSimilarity *= scale_constant;
        mapping.setLocalSimilarity(localSimilarity);
        
        double related_ness = 0.0;
        for(Mention mention2: mentionMappings.keySet()){
          if(mention.getMention().equalsIgnoreCase(mention2.getMention()) == false || 
              mention.getType().equalsIgnoreCase(mention2.getType()) == false){
            List<MentionEntityMapping> mps = mentionMappings.get(mention2);
            for(MentionEntityMapping mp: mps){
              Entity e = mp.getEntity();
              long key = concatenate(entity.getId(), e.getId());
              if(coh.containsKey(key))
                related_ness += coh.get(key);
            }
          }
        }
        
        if(related_ness > max_relatedness)
          max_relatedness = related_ness;
        
        double weight = entity.getLocalSimilarity() + related_ness;
        EntityNode entityNode = new EntityNode(entity, mention, weight);
        candidates.add(entityNode);
      }
			
			degree.put(mention, mappings.size() - counter);
    }
		
		if(max_relatedness > 0) {
		  Iterator<EntityNode> iterator = candidates.iterator();
	    while(iterator.hasNext()){
	      EntityNode en = iterator.next();
	      double related_ness = en.getWeight() - en.getEntity().getLocalSimilarity();
	      en.setWeight((related_ness / max_relatedness) + en.getEntity().getLocalSimilarity());
	    }
		}
			
		
		// main loop.
		while(!candidates.isEmpty()){
			EntityNode removedNode = candidates.poll();
			Mention mention = removedNode.getMention();
			Entity entity = removedNode.getEntity();
			
			int id = entity.getId();
			int d = degree.get(mention);
			if(d > 1){
				degree.put(mention, d-1);
				// update
				PriorityQueue<EntityNode> candidates_tmp = new PriorityQueue<EntityNode>();
				Iterator<EntityNode> iter = candidates.iterator();
				while(iter.hasNext()){
					EntityNode en = iter.next();
					if(mention.getMention().equalsIgnoreCase(en.getMention().getMention()) == false || 
					    mention.getType().equalsIgnoreCase(en.getMention().getType()) == false){
						Entity e = en.getEntity();
						int e_id = e.getId();
            long min = Math.min(id, e_id);
            long max = id + e_id - min;
            long key = concatenate(min, max);
            en.setWeight(en.getWeight() - (coh.get(key) / max_relatedness));
						
					}
					candidates_tmp.add(en);
				}
				candidates = candidates_tmp;
			}
			else{
			  results.put(mention, entity);
//			  if(testPreRecall) {
//			    // find the final entity for this mention
//	        if(entity.getFirstLocalSim() > threshold){
//	          results.put(mention, entity);
//	        }
//	        else if(final_round){
//	          results.put(mention, new Entity("UNCLEAR", -2));
//	        }
//			  }
//			  
//			  else {
////			    if(Settings.getTestPrecisionOption())
////			      threshold = 0;
//			    // find the final entity for this mention
//	        if(entity.getLocalSimilarity() > threshold || (final_round && entity.getName().replace("_", " ").equalsIgnoreCase(entity.getMention().getMention()))){
//	          results.put(mention, entity);
//	        }
//			  }
			  
			}
			
		}
		
		return results;
	}
	
	
}
