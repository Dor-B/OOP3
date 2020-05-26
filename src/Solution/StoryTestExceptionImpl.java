package Solution;

import Provided.StoryTestException;

import java.util.List;

public class StoryTestExceptionImpl extends StoryTestException {
    private int numFail;
    private String sentence;
    private List<String> storyExpected;
    private List<String> testResult;

    public StoryTestExceptionImpl(String sentence, List<String> storyExpected, List<String> testResult) {
        this.sentence = sentence;
        this.storyExpected = storyExpected;
        this.testResult = testResult;
    }

    public void setNumFail(int numFail) {
        this.numFail = numFail;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public void setStoryExpected(List<String> storyExpected) {
        this.storyExpected = storyExpected;
    }

    public void setTestResult(List<String> testResult) {
        this.testResult = testResult;
    }
    @Override
    public String getSentence() {
        return sentence;
    }

    @Override
    public List<String> getStoryExpected() {
        return storyExpected;
    }

    @Override
    public List<String> getTestResult() {
        return testResult;
    }

    @Override
    public int getNumFail() {
        return numFail;
    }
}
