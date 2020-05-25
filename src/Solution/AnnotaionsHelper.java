package Solution;

import java.util.LinkedList;
import java.util.List;

public class AnnotaionsHelper {
    static private String getFirstWords(String str){
        return str.substring(0, str.lastIndexOf(" "));
    }
    static private String getLastWord(String str){
        return str.substring(str.lastIndexOf(" ") + 1);
    }
    /**
     * Get a list of params matching pattern (without "or", "Given", "Then", "When")
     * if sentence matches pattern (otherwise empty list)
     * @param filledSentence e.g "a classroom with a capacity of 75 and size of large"
     * @param pattern e.g "a classroom with a capacity of &capacity and size of &size"
     * @return list of parameters (in this example ["75", "large"]) or an empty list if
     * filledSentence and pattern do not agree.
     */
    static public List<String> getParamsBySentenceII(String filledSentence, String pattern){
        String[] andSeperatedFilled = filledSentence.split(" and ");
        String[] andSeperatedPattern = pattern.split(" and ");
        if(andSeperatedFilled.length != andSeperatedPattern.length)
            return new LinkedList<>();
        List<String> res = new LinkedList<>();
        for (int i = 0; i < andSeperatedPattern.length; i++) {
            String withoutParamPattern = getFirstWords(andSeperatedPattern[i]);
            String withoutParamFilled = getFirstWords(andSeperatedFilled[i]);
            if(!withoutParamPattern.equals(withoutParamFilled)){ // check if same pattern
                return new LinkedList<>();
            }
//            String paramPattern = getLastWord(andSeperatedPattern[i]);
            String paramFilled = getLastWord(andSeperatedFilled[i]);
            res.add(paramFilled);
        }
        return res;
    }

    static public String removeFirstWord(String line){
        return line.substring(line.indexOf(" ") + 1);
    }

    static private String[] separateByOr(String sentence){
        return sentence.split(" or ");
    }
}
