package Solution;

import Provided.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import Solution.AnnotaionsHelper;
import org.junit.ComparisonFailure;

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
    public Object copyTestObject(Object testObj) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException{
        Object backup = testObj.getClass().newInstance();
        for(Field field : testObj.getClass().getFields()){
            field.setAccessible(true);
            Object fieldVal = field.get(testObj);
            if(fieldVal instanceof Cloneable){
                field.set(backup, fieldVal.getClass().getMethod("clone").invoke(fieldVal));
            }else{
                try{
                    Constructor copyConstructor = fieldVal.getClass().getConstructor(fieldVal.getClass());
                    field.set(backup, copyConstructor.newInstance(fieldVal));
                }catch (NoSuchMethodException exp){
                    field.set(backup, fieldVal);
                }
            }
        }
        return backup;
    }
    public static Object[] stringParamsToGeneral(List<String> params){
        return params.stream()
                .map(p -> (Object) p)
                // cast to integer if necessary
                .map(o -> ((String) o).matches("-?\\d+") ?
                        Integer.parseInt((String) o) : o).toArray();
    }
    public void searchAndInvoke(Object testObj, String lineWithoutBeginning, annotationType annoType) throws WordNotFoundException {
        Tuple<Method, List<String>> methodData = searchAnnotation(testObj.getClass(), lineWithoutBeginning, annotationType.WHEN);
        if(methodData == null){
            switch (annoType){
                case THEN:
                    throw new ThenNotFoundException();
                case WHEN:
                    throw new WhenNotFoundException();
                case GIVEN:
                    throw new GivenNotFoundException();
            }
        }
        Method method = methodData.first;
        Object[] params = stringParamsToGeneral(methodData.second);
        try {
            method.invoke(testObj, params);
        }catch (InvocationTargetException ite){
            if(ite.getCause() instanceof ComparisonFailure){
                throw (ComparisonFailure) ite.getCause();
            }
        }catch (Exception e){ // should not be here
            e.printStackTrace();
        }
    }
    public void testWhenThenOnObject(Object testObj, WhenThenStruct whenThenStruct) throws WordNotFoundException, StoryTestExceptionImpl {
        Object backup = null;
        try{
            backup = copyTestObject(testObj);
        }catch (Exception e){ // should not be here
            e.printStackTrace();
            return;
        }
        for(String whenLine : whenThenStruct.whens){
            whenLine = AnnotaionsHelper.removeFirstWord(whenLine);
            searchAndInvoke(testObj, whenLine, annotationType.WHEN);
        }
        StoryTestExceptionImpl testException = null;
        boolean noErrorsSoFar = true;
        int numFails = 0;
        for(String thenLine : whenThenStruct.thens){
            String thenLineClean = AnnotaionsHelper.removeFirstWord(thenLine);
            String[] orSeparated = AnnotaionsHelper.separateByOr(thenLineClean);
            List<String> storyExpected = new LinkedList<>();
            List<String> storyActual = new LinkedList<>();
            boolean foundGood = false;
            for(String thenPart : orSeparated){
                try{
                    searchAndInvoke(testObj, thenPart, annotationType.THEN);
                    foundGood = true;
                    break;
                }catch (ComparisonFailure cmpFailErr){
                    testObj = backup;
                    if(noErrorsSoFar){
                        storyExpected.add(cmpFailErr.getExpected());
                        storyActual.add(cmpFailErr.getActual());
                    }
                }
            }
            if(!foundGood){ // all
                if(noErrorsSoFar){ // this is the first then to fail
                    noErrorsSoFar = false;
                    testException = new StoryTestExceptionImpl(thenLine, storyExpected, storyActual);
                }
                numFails++;
                testException.setNumFail(numFails);
            }
        }
        if(testException != null){
            throw testException;
        }
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
