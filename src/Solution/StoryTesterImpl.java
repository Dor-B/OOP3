package Solution;

import Provided.*;

import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.List;

import org.junit.ComparisonFailure;

enum annotationType{
    GIVEN,
    WHEN,
    THEN,
}

public class StoryTesterImpl implements StoryTester {

    @Override
    public void testOnInheritanceTree(String story, Class<?> testClass) throws Exception {
        if(story == null || testClass == null){
            throw new IllegalArgumentException();
        }
        StoryStruct currStoryStruct = new StoryStruct(story);
        Object instance = getInstance(testClass);
        String line = AnnotaionsHelper.removeFirstWord(currStoryStruct.givenSentence);
        searchAndInvoke(instance,line,annotationType.GIVEN);
        boolean noFailsSoFar = true;
        StoryTestExceptionImpl exp = null;
        for(WhenThenStruct whenThen:currStoryStruct.whenThenGroups) {
            Tuple3<Integer, StoryTestExceptionImpl,Object> res = testWhenThenOnObject(instance,whenThen,noFailsSoFar);
            instance = res.third;
            if(noFailsSoFar&&res.first!=0){
                noFailsSoFar=false;
                exp=res.second;
            }
            if(exp!=null)
                exp.setNumFail(exp.getNumFail()+res.first);
        }
        if(exp!=null)
            throw exp;
    }

    /**
     * @param story- the story to test on the class
     * @param testClass- the calss on which the testing will commence
     * @return nothing, if there was a failure throws an exception
     */
    @Override
    public void testOnNestedClasses(String story, Class<?> testClass) throws Exception {
        try{
            testOnInheritanceTree(story,testClass);
        }catch (GivenNotFoundException exp){//if given sentence was not found in the given in the class serches in her nested classes
            Class<?>[] nested = testClass.getDeclaredClasses();
            for(Class<?> c:nested){
                try{
                    testOnNestedClasses(story,c);
                    return;
                }catch (GivenNotFoundException $){
                    continue;
                }
            }
            throw new GivenNotFoundException();
        }

    }

    /**
     * Copy an object - try clone, than copy c'tor, then assignment
     * @param testObj the object to copy
     * @return new copied object
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public Object copyTestObject(Object testObj) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException{
        Object backup = getInstance(testObj.getClass());

        for(Field field : testObj.getClass().getDeclaredFields()){
            field.setAccessible(true);
            Object fieldVal = field.get(testObj);
            // try clone()
            if(fieldVal instanceof Cloneable){
                Method cloneMethod = fieldVal.getClass().getMethod("clone");
                cloneMethod.setAccessible(true);
                field.set(backup, cloneMethod.invoke(fieldVal));
            }else{
                // try copy c'tor
                try{
                    Constructor<?> copyConstructor = fieldVal.getClass().getDeclaredConstructor(fieldVal.getClass());
                    copyConstructor.setAccessible(true);
                    field.set(backup, copyConstructor.newInstance(fieldVal));
                // if none above worked use assignment
                }catch (Exception exp){
                    field.set(backup, fieldVal);
                }
            }
        }
        return backup;
    }

    /**
     * Convert String list to array of Integer (where possible) and String
     */
    public static Object[] stringParamsToGeneral(List<String> params){
        return params.stream()
                .map(p -> (Object) p)
                // cast to integer if necessary
                .map(o -> ((String) o).matches("-?\\d+") ?
                        Integer.parseInt((String) o) : o).toArray();
    }

    /**
     * Search and invoke a story line in the test's object class. When searching it's going up in the inheritance tree
     * @param testObj the test object
     * @param lineWithoutBeginning the story line without the prefix "GIVEN"\"THEN"\"WHEN"
     * @param annoType type of story line GIVEN\THEN\WHEN
     * @throws WordNotFoundException if line is not found in the test's object class or in upper class
     */
    public void searchAndInvoke(Object testObj, String lineWithoutBeginning, annotationType annoType) throws WordNotFoundException {
        Tuple<Method, List<String>> methodData = searchAnnotation(testObj.getClass(), lineWithoutBeginning, annoType);
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
        method.setAccessible(true);
        try {
            method.invoke(testObj, params);
        }catch (InvocationTargetException ite){
            if(ite.getCause() instanceof ComparisonFailure){
                throw (ComparisonFailure) ite.getCause();
            }
        }catch (Exception e){ // should not be here
            //e.printStackTrace();
        }
    }

    /**
     * Run one group of [then,...,then,when,...,when] lines on a test object
     * @param testObj the test object of the according test class
     * @param whenThenStruct the WhenThenStruct that holds then and when lines
     * @param noErrorsSoFar set this to true to make the function return the first "Then" fail for future use
     * @return a Tuple3 of:
     *              .first [Integer] = how many "Then" fails were in this when-then group
     *              .second [StoryTestExceptionImpl] = the exception returned by the first "Then" fail (if noErrorsSoFar is true)
     *              .third [Object] = the updated testObj (relevant if backup happened and changed it
     * @throws WordNotFoundException if one of the story lines were not found
     */
    public Tuple3<Integer, StoryTestExceptionImpl, Object> testWhenThenOnObject(Object testObj, WhenThenStruct whenThenStruct, boolean noErrorsSoFar) throws WordNotFoundException {
        Object backup;
        try{
            backup = copyTestObject(testObj);
        }catch (Exception e){ // should not be here
            //e.printStackTrace();
            return new Tuple3<Integer,StoryTestExceptionImpl,Object>(0,null,null);
        }
        // iterate over the whens
        for(String whenLine : whenThenStruct.whens){
            whenLine = AnnotaionsHelper.removeFirstWord(whenLine);
            searchAndInvoke(testObj, whenLine, annotationType.WHEN);
        }
        StoryTestExceptionImpl testException = null;
        int numFails = 0;
        // iterate over the thens
        for(String thenLine : whenThenStruct.thens){
            String thenLineClean = AnnotaionsHelper.removeFirstWord(thenLine);
            String[] orSeparated = AnnotaionsHelper.separateByOr(thenLineClean);
            // lists to store the fails actual vs expected
            List<String> storyExpected = new LinkedList<>();
            List<String> storyActual = new LinkedList<>();
            // remember if one of the sentences separated by "or" was successful
            boolean foundGood = false;
            // in each then iterate over the sentences separated by "or"
            for(String thenPart : orSeparated){
                try{
                    searchAndInvoke(testObj, thenPart, annotationType.THEN);
                    foundGood = true;
                    break;
                }catch (ComparisonFailure cmpFailErr){
                    if(noErrorsSoFar){
                        storyExpected.add(cmpFailErr.getExpected());
                        storyActual.add(cmpFailErr.getActual());
                    }
                }
            }
            if(!foundGood){ // all
                testObj = backup;
                if(noErrorsSoFar){ // this is the first then to fail
                    noErrorsSoFar = false;
                    testException = new StoryTestExceptionImpl(thenLine, storyExpected, storyActual);
                }
                numFails++;
            }
        }
        return new Tuple3<Integer, StoryTestExceptionImpl, Object>(numFails,testException,testObj);
    }

    /**
     * Store a group of lines [when,...,when,then,...,then]
     */
    static public class WhenThenStruct {
        List<String> whens;
        List<String> thens;

        public WhenThenStruct(List<String> whens, List<String> thens) {
            this.whens = whens;
            this.thens = thens;
        }
    }

    /**
     * Convert a story string to:
     * 1. Given sentence
     * 2. Groups of lines starting with [When,...,When,Then,...,Then]
     */
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

    /**
    *   recieves the whole line excluding the annotation name
    *   searches for the correct method given an annotation
    *   and returns a tuple of the method and a list of parameters.
    */
    public Tuple<Method,List<String>> searchAnnotation (Class<?> testClass, String givenString,annotationType type)throws GivenNotFoundException, ThenNotFoundException, WhenNotFoundException {
        //when gets to the end of the recursion throws the appropriate exception
        if(testClass==null) {
            switch (type) {
                case GIVEN:
                    throw new GivenNotFoundException();
                case THEN:
                    throw new ThenNotFoundException();
                case WHEN:
                    throw new WhenNotFoundException();
            }
        }
        Method[] methods = testClass.getDeclaredMethods();
        String AnoString = null;
        //checks all the methods to find the one fitting the annotation
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
            //if an appropriate method was found returns a tuple which includes the method and it parameters
            if(AnoString!=null) {
                List<String> params = AnnotaionsHelper.getParamsBySentenceII(givenString, AnoString);
                if (!params.isEmpty()) {
                    return new Tuple<>(method, params);
                }
            }
        }
        // if an appropriate method was no found,
        // the function searches for the method in the superclass of the given class
        return searchAnnotation(testClass.getSuperclass(),givenString,type);
    }

    /**
    *   the function returns an instance of the given class
    *   even if the class is static or the constructor is private
    */
    public Object getInstance(Class<?> clazz){
        Object instance=null;
        Constructor<?> ctor= null;
        try {
            // checks if the class is static if so find the appropriate constructor
            // which gets an instance of the class
            if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
                ctor = clazz.getDeclaredConstructor(clazz.getDeclaringClass());
                ctor.setAccessible(true);
                instance = ctor.newInstance(getInstance(clazz.getDeclaringClass()));
            } else {//if the class is not static finds the parameter-less constructor
                ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                instance = ctor.newInstance();
            }
        } catch(Exception e){//should not get here
            //e.printStackTrace();
        }
        return instance;
    }

    //tuple like calss of 2 objects
    static public class Tuple<X, Y> {
        public final X first;
        public final Y second;
        public Tuple(X first, Y second) {
            this.first = first;
            this.second = second;
        }
    }

    //tuple like calss of 3 objects
    static public class Tuple3<X, Y, Z> {
        public final X first;
        public final Y second;
        public final Z third;
        public Tuple3(X first, Y second ,Z third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

}
