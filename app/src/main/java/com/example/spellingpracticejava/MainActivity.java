package com.example.spellingpracticejava;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String DATABASE_NAME = "SpellingBeeDatabase";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_WORDS = "Words";
    private static final String COLUMN_WORD = "Word";
    private static final String COLUMN_CORRECT_ATTEMPTS = "CorrectAttempts";
    private static final String COLUMN_INCORRECT_ATTEMPTS = "IncorrectAttempts";

    private SQLiteDatabase database;

    private TextToSpeech textToSpeech;
    private Spinner selectedWordListSpinner;
    private TextView progressTextView;
    private EditText spellingGuessEntry;
    private String currentWordToGuess;
    private Set<String> allWordsSet;
    private Set<String> correctWordsSet;
    private Set<String> incorrectWordsSet;
    private Set<String> neverTriedWordsSet;
    private ImageView dancingDogImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Text-to-Speech engine
        textToSpeech = new TextToSpeech(this, this);

        // Initialize UI components
        selectedWordListSpinner = findViewById(R.id.selectedWordListSpinner);
        progressTextView = findViewById(R.id.progressTextView);
        spellingGuessEntry = findViewById(R.id.spellingGuessEntry);

        // Initialize word sets
        allWordsSet = loadWordsFromTextFile("MySpellingWords.txt");
        correctWordsSet = new HashSet<>();
        incorrectWordsSet = new HashSet<>();
        neverTriedWordsSet = new HashSet<>(allWordsSet);

        // Initialize spinner with word lists
        List<String> wordLists = new ArrayList<>();
        wordLists.add("neverTriedWords");
        wordLists.add("allWords");
        wordLists.add("correctWords");
        wordLists.add("incorrectWords");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wordLists);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectedWordListSpinner.setAdapter(adapter);
        selectedWordListSpinner.setSelection(adapter.getPosition("neverTriedWords"));

        // Initialize "Say the Next Word" button
        Button pronounceButton = findViewById(R.id.pronounceButton);
        pronounceButton.setOnClickListener(v -> pronounceNextWord());

        // Initialize "Submit Guess" button
        Button submitGuessButton = findViewById(R.id.submitGuessButton);
        submitGuessButton.setOnClickListener(v -> checkSpelling());

        // Initialize "Reset All" button
        Button resetAllButton = findViewById(R.id.resetAllButton);
        resetAllButton.setOnClickListener(v -> showResetConfirmationDialog());

        dancingDogImageView = findViewById(R.id.dancingDogImageView);


        updateProgress();
    }

    private Set<String> loadWordsFromTextFile(String fileName) {

        Set<String> words = new HashSet<>();
        BufferedReader reader = null;

        try {
            // Open the file using InputStream
            InputStream inputStream = getAssets().open(fileName);

            // Create a BufferedReader to read the file
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read each line from the file and add it to the set
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the BufferedReader
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return words;
    }

    private void pronounceNextWord() {
        String selectedList = selectedWordListSpinner.getSelectedItem().toString();
        Set<String> wordList;
        hideDancingDog();
        switch (selectedList) {
            case "neverTriedWords":
                wordList = neverTriedWordsSet;
                break;
            case "allWords":
                wordList = allWordsSet;
                break;
            case "correctWords":
                wordList = correctWordsSet;
                break;
            case "incorrectWords":
                wordList = incorrectWordsSet;
                break;
            default:
                wordList = neverTriedWordsSet;
        }

        if (!wordList.isEmpty()) {
            List<String> list = new ArrayList<>(wordList);
            Collections.shuffle(list);
            currentWordToGuess = list.get(0);
            pronounceWord(currentWordToGuess);
        } else {
            Toast.makeText(this, "No words in the selected list", Toast.LENGTH_SHORT).show();
        }
    }

    private void pronounceWord(String word) {
        textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, "pronunciation");
    }

    private void checkSpelling() {
        String userGuess = spellingGuessEntry.getText().toString().trim().toLowerCase();

        if (userGuess.equals(currentWordToGuess.toLowerCase())) {
            // Correct guess
            Toast.makeText(this, "Correct guess! You rock", Toast.LENGTH_SHORT).show();
            correctWordsSet.add(currentWordToGuess);
            neverTriedWordsSet.remove(currentWordToGuess);
            incorrectWordsSet.remove(currentWordToGuess);
            showDancingDog();

        } else {
            // Incorrect guess
            Toast.makeText(this, "Bad guess! Sorry!", Toast.LENGTH_SHORT).show();
            incorrectWordsSet.add(currentWordToGuess);
            neverTriedWordsSet.remove(currentWordToGuess);
            correctWordsSet.remove(currentWordToGuess);
        }

        // Update UI and reset entry
        updateProgress();
        spellingGuessEntry.setText("");
    }

    private void updateProgress() {
        int allWords = allWordsSet.size();
        int correctWords = correctWordsSet.size();
        int incorrectWords = incorrectWordsSet.size();
        int neverTriedWords = neverTriedWordsSet.size();

        String progressText = String.format(Locale.getDefault(),
                "Total Words: %d\nCorrect Words: %d\nIncorrect Words: %d\nNever Tried Words: %d",
                allWords, correctWords, incorrectWords, neverTriedWords);

        progressTextView.setText(progressText);
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset All")
                .setMessage("Are you sure you want to reset the app?")
                .setPositiveButton("Yes", (dialog, which) -> resetAll())
                .setNegativeButton("No", null)
                .show();
    }

    private void resetAll() {
        // Reset allWordsSet to be the same as the original allWords list
        neverTriedWordsSet.clear();
        neverTriedWordsSet.addAll(allWordsSet);

        // Set correctWordsSet and incorrectWordsSet back to empty sets
        correctWordsSet.clear();
        incorrectWordsSet.clear();

        // Update UI
        updateProgress();

        // Pronounce the next word after resetting
        pronounceNextWord();
    }

    private void showDancingDog() {
        dancingDogImageView.setVisibility(View.VISIBLE);
    }

    private void hideDancingDog() {
        dancingDogImageView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeech", "Language not supported");
            }
        } else {
            Log.e("TextToSpeech", "Initialization failed");
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
