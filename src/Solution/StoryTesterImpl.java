package Solution;

import Provided.StoryTester;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import Solution.AnnotaionsHelper;
enum annotationType{
    GIVEN,
    WHEN,
    THEN,
}

public class StoryTesterImpl implements StoryTester {
    @Override
    public void testOnInheritanceTree(String story, Class<?> testClass) throws Exception {
        //story parsing here


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


    public Tuple<Method,List<String>> searchAnnotation (Class<?> testClass, String givenString,annotationType type) {
        if(testClass==null)
            return null;
        Method[] methods = testClass.getDeclaredMethods();
        givenString = AnnotaionsHelper.removeFirstWord(givenString);
        String AnoString = "";
        for (Method method : methods) {
            switch (type) {
                case GIVEN:
                    Given currAno = method.getAnnotation(Given.class);
                    if (currAno != null)
                        AnoString = currAno.value();
                    break;
                case WHEN:
                    When currAno2 = method.getAnnotation(When.class);
                    if (currAno2 != null)
                        AnoString = currAno2.value();
                    break;
                case THEN:
                    Then currAno3 = method.getAnnotation(Then.class);
                    if (currAno3 != null)
                        AnoString = currAno3.value();
                    break;
            }
            List<String> params = AnnotaionsHelper.getParamsBySentenceII(givenString, AnoString);
            if (!params.isEmpty()) {
                return new Tuple<>(method, params);
            }
        }
        return searchAnnotation(testClass.getSuperclass(),givenString,type);
    }

    static public class Tuple<X, Y> {
        public final X first;
        public final Y second;
        public Tuple(X first, Y second) {
            this.first = first;
            this.second = second;
        }
    }

}
