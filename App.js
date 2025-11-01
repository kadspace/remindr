import { useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

export default function App() {
  const [reminderTitle, setReminderTitle] = useState('');
  const [reminderWhen, setReminderWhen] = useState('');
  const [reminders, setReminders] = useState([]);

  const [noteTitle, setNoteTitle] = useState('');
  const [noteBody, setNoteBody] = useState('');
  const [notes, setNotes] = useState([]);

  const addReminder = () => {
    if (!reminderTitle.trim() || !reminderWhen.trim()) {
      Alert.alert('Almost there!', 'Please add a title and when to be reminded.');
      return;
    }

    setReminders((current) => [
      ...current,
      {
        id: Date.now().toString(),
        title: reminderTitle.trim(),
        when: reminderWhen.trim(),
      },
    ]);

    setReminderTitle('');
    setReminderWhen('');
  };

  const addNote = () => {
    if (!noteTitle.trim() && !noteBody.trim()) {
      Alert.alert('Need a little more', 'Write a title or some note text first.');
      return;
    }

    setNotes((current) => [
      ...current,
      {
        id: Date.now().toString(),
        title: noteTitle.trim(),
        body: noteBody.trim(),
      },
    ]);

    setNoteTitle('');
    setNoteBody('');
  };

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <StatusBar style="auto" />
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.title}>Remindr</Text>
        <Text style={styles.subtitle}>
          Capture notes or queue a reminder to come back later.
        </Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>New reminder</Text>
          <TextInput
            style={styles.input}
            placeholder="Reminder title"
            value={reminderTitle}
            onChangeText={setReminderTitle}
          />
          <TextInput
            style={styles.input}
            placeholder="When should we nudge you? (e.g. 2024-08-15 18:30)"
            value={reminderWhen}
            onChangeText={setReminderWhen}
          />
          <Text style={styles.helperText}>
            This is a lightweight prototype. Enter any text that helps you
            remember when the reminder should fire.
          </Text>
          <Pressable style={styles.button} onPress={addReminder}>
            <Text style={styles.buttonLabel}>Save reminder</Text>
          </Pressable>
          {reminders.length > 0 && (
            <View style={styles.list}>
              {reminders.map((reminder) => (
                <View key={reminder.id} style={styles.card}>
                  <Text style={styles.cardTitle}>{reminder.title}</Text>
                  <Text style={styles.cardMeta}>{reminder.when}</Text>
                </View>
              ))}
            </View>
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Quick note</Text>
          <TextInput
            style={styles.input}
            placeholder="Note title"
            value={noteTitle}
            onChangeText={setNoteTitle}
          />
          <TextInput
            style={[styles.input, styles.multilineInput]}
            placeholder="Write anything you need to remember..."
            value={noteBody}
            onChangeText={setNoteBody}
            multiline
            numberOfLines={4}
            textAlignVertical="top"
          />
          <Pressable style={styles.button} onPress={addNote}>
            <Text style={styles.buttonLabel}>Save note</Text>
          </Pressable>
          {notes.length > 0 && (
            <View style={styles.list}>
              {notes.map((note) => (
                <View key={note.id} style={styles.card}>
                  {!!note.title && (
                    <Text style={styles.cardTitle}>{note.title}</Text>
                  )}
                  {!!note.body && <Text style={styles.cardBody}>{note.body}</Text>}
                </View>
              ))}
            </View>
          )}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: '#f6f6f6' },
  container: {
    paddingHorizontal: 24,
    paddingBottom: 48,
    paddingTop: 72,
    gap: 24,
  },
  title: {
    fontSize: 32,
    fontWeight: '600',
    textAlign: 'center',
  },
  subtitle: {
    marginTop: 12,
    fontSize: 16,
    textAlign: 'center',
    color: '#555',
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 20,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
    gap: 12,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    backgroundColor: '#fafafa',
  },
  multilineInput: {
    minHeight: 120,
  },
  helperText: {
    fontSize: 13,
    color: '#666',
  },
  button: {
    backgroundColor: '#2563eb',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
  },
  buttonLabel: {
    color: '#fff',
    fontWeight: '600',
  },
  list: {
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#eee',
    paddingTop: 12,
    gap: 12,
  },
  card: {
    backgroundColor: '#f9fafb',
    borderRadius: 10,
    padding: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    gap: 6,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#111827',
  },
  cardMeta: {
    fontSize: 14,
    color: '#374151',
  },
  cardBody: {
    fontSize: 15,
    color: '#1f2937',
  },
});
