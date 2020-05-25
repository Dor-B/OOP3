package Solution;

import Provided.StoryTester;

import java.util.LinkedList;
import java.util.List;

public class StoryTesterImpl implements StoryTester {
    @Override
    public void testOnInheritanceTree(String story, Class<?> testClass) throws Exception {
        //TODO: Implement
    }

    @Override
    public void testOnNestedClasses(String story, Class<?> testClass) throws Exception {
        //TODO: Implement
    }

    static public class WhenThenStruct {
        List<String> whens;
        List<String> thens;

        public WhenThenStruct(List<String> whens, List<String> thens) {
            this.whens = whens;
            this.thens = thens;
        }
    }

    static public class StoryStruct{
        String givenSentence;
        List<WhenThenStruct> whenThenGroups;
        public StoryStruct(String story) {
            String[] lines = story.split("\\r?\\n");
            givenSentence = lines[0];
            whenThenGroups = new LinkedList<>();
            boolean collectingWhen = true;
            List<String> whens = new LinkedList<>();
            List<String> thens = new LinkedList<>();
            // skip Given and iterate over sentences
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.startsWith("When")) {
                    // if When came after Then, append the when-then group to the list
                    if(!collectingWhen){
                        whenThenGroups.add(new WhenThenStruct(whens, thens));
                        whens = new LinkedList<>();
                        thens = new LinkedList<>();
                    }
                    whens.add(line);
                    collectingWhen = true;
                } else if (line.startsWith("Then")) {
                    thens.add(line);
                    collectingWhen = false;
                }
            }
            // add the last batch
            whenThenGroups.add(new WhenThenStruct(whens, thens));
        }
    }

}
