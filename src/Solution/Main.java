package Solution;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        StoryTesterImpl.StoryStruct s = new  StoryTesterImpl.StoryStruct(
            "Given a classroom with a capacity of 50\n" +
            "When the number of students in the classroom is 40\n" +
            "When the number of broken chairs in the classroom is 10\n" +
            "Then the classroom is full\n" +
            "When the number of students in the classroom is 40\n" +
            "Then the classroom is not-full"
        );
        System.out.println("GIVEN: " + s.givenSentence);
        for(StoryTesterImpl.WhenThenStruct ws : s.whenThenGroups){
            System.out.println("---------GROUP--------------");
            System.out.println("+++++++++WHENS++++++++++++++");
            for(String when :  ws.whens)
                System.out.println(when);
            System.out.println("+++++++++THENS++++++++++++++");
            for(String then :  ws.thens)
                System.out.println(then);
        }
        List<String> params = AnnotaionsHelper.getParamsBySentenceII(
                "the number of students in the classroom is 1378 and the number among them that are standing is 42",
                "the number of students in the classroom is &students and the number among them that are standing is &standing"
        );
        System.out.println(params);
    }
}
