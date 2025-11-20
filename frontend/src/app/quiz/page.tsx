'use client';
import { useState, useEffect } from 'react';
import { useQuiz } from '@/hooks/useQuiz';
import { useAuthContext } from '@/contexts/AuthContext';
import { useDocuments } from '@/hooks/useDocuments';
import { apiClient } from '@/services/api';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Alert } from '@/components/ui/alert';
import {
  BookOpen, 
  CheckCircle2, 
  XCircle, 
  Trophy, 
  RefreshCw, 
  History,
  FileText,
  Brain,
  Loader2,
  ChevronRight,
  Upload
} from 'lucide-react';

export const QuizPage = () => {
  const { user } = useAuthContext();
  const { quiz, result, submissions, isLoading, isSubmitting, error, createQuiz, submitQuiz, loadSubmissions, resetQuiz } = useQuiz(user?.id || '');
  const { documents, loadDocuments } = useDocuments(user?.id || '');

  const [view, setView] = useState<'create' | 'quiz' | 'result' | 'history'>('create');
  const [quizTitle, setQuizTitle] = useState('');
  const [documentText, setDocumentText] = useState('');
  const [selectedDocument, setSelectedDocument] = useState<string | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState<Record<number, string>>({});
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isParsingFile, setIsParsingFile] = useState(false);

  useEffect(() => {
    if (user?.id) {
      loadDocuments();
      loadSubmissions();
    }
  }, [user?.id, loadDocuments, loadSubmissions]);

  const handleCreateQuiz = async () => {
    if (!quizTitle.trim() || !documentText.trim()) {
      return;
    }

    const newQuiz = await createQuiz(quizTitle, documentText);
    if (newQuiz) {
      setView('quiz');
      setCurrentQuestionIndex(0);
      setUserAnswers({});
    }
  };

  const handleSelectDocument = async (docId: string) => {
    setSelectedDocument(docId);
    const doc = documents.find(d => d.documentId === docId);
    if (doc) {
      setQuizTitle(`Quiz: ${doc.fileName}`);

      // Fetch the full document content from the backend
      try {
        const fullDoc = await apiClient.getDocument(docId);
        if (fullDoc.extractedText) {
          setDocumentText(fullDoc.extractedText);
        } else {
          setDocumentText(`[Content for ${doc.fileName}]\n\nDocument is still being processed or has no text content.`);
        }
      } catch (error) {
        console.error('Error fetching document content:', error);
        setDocumentText(`[Error loading ${doc.fileName}]\n\nPlease try uploading the document again or paste text manually.`);
      }
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploadedFile(file);
    setIsParsingFile(true);

    try {
      const text = await parseFileContent(file);
      setDocumentText(text);
      setQuizTitle(`Quiz: ${file.name.replace(/\.[^/.]+$/, '')}`);
    } catch (error) {
      console.error('Error parsing file:', error);
      alert('Failed to parse file. Please try a different file or paste text manually.');
    } finally {
      setIsParsingFile(false);
    }
  };

  const parseFileContent = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = (e) => {
        const content = e.target?.result as string;

        // Handle different file types
        if (file.type === 'text/plain' || file.name.endsWith('.txt')) {
          resolve(content);
        } else if (file.type === 'application/json' || file.name.endsWith('.json')) {
          resolve(content);
        } else if (file.name.endsWith('.md')) {
          resolve(content);
        } else {
          // For other types, just try to read as text
          resolve(content);
        }
      };

      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsText(file);
    });
  };

  const handleSelectAnswer = (questionId: number, answer: string) => {
    setUserAnswers(prev => ({
      ...prev,
      [questionId]: answer
    }));
  };

  const handleNext = () => {
    if (quiz && currentQuestionIndex < quiz.questions.length - 1) {
      setCurrentQuestionIndex(prev => prev + 1);
    }
  };

  const handlePrevious = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(prev => prev - 1);
    }
  };

  const handleSubmitQuiz = async () => {
    if (!quiz) return;

    const quizResult = await submitQuiz(quiz.id, userAnswers);
    if (quizResult) {
      // Reload submissions to update history in real-time
      await loadSubmissions();
      setView('result');
    }
  };

  const handleStartNew = () => {
    resetQuiz();
    setView('create');
    setQuizTitle('');
    setDocumentText('');
    setSelectedDocument(null);
    setUserAnswers({});
    setCurrentQuestionIndex(0);
    setUploadedFile(null);
  };

  const currentQuestion = quiz?.questions[currentQuestionIndex];
  const progress = quiz ? ((currentQuestionIndex + 1) / quiz.questions.length) * 100 : 0;
  const allQuestionsAnswered = quiz ? quiz.questions.every(q => userAnswers[q.id]) : false;

  // Create Quiz View
  if (view === 'create') {
    return (
      <div className="flex flex-col h-full w-full overflow-y-auto p-6 space-y-6">
        <div className="max-w-4xl mx-auto w-full space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold flex items-center gap-2">
                <Brain className="w-8 h-8 text-primary" />
                Create a Quiz
              </h1>
              <p className="text-muted-foreground mt-2">
                Generate an adaptive quiz from your documents
              </p>
            </div>
            <Button 
              variant="outlined"
              onClick={() => setView('history')}
              className="gap-2"
            >
              <History className="w-4 h-4" />
              View History
            </Button>
          </div>

          {error && (
            <Alert className="border-destructive bg-destructive/10 text-destructive">
              {error}
            </Alert>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Select Document or Enter Text</CardTitle>
              <CardDescription>
                Choose a document from your library or paste text directly
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* File Upload */}
              <div className="space-y-2">
                <label className="text-sm font-medium">Upload Document</label>
                <div className="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-primary transition-colors">
                  <input
                    type="file"
                    id="file-upload"
                    className="hidden"
                    accept=".txt,.md,.json,.pdf,.doc,.docx"
                    onChange={handleFileUpload}
                    disabled={isParsingFile}
                  />
                  <label
                    htmlFor="file-upload"
                    className="cursor-pointer flex flex-col items-center gap-2"
                  >
                    {isParsingFile ? (
                      <>
                        <Loader2 className="w-8 h-8 text-primary animate-spin" />
                        <span className="text-sm text-muted-foreground">Parsing file...</span>
                      </>
                    ) : uploadedFile ? (
                      <>
                        <FileText className="w-8 h-8 text-green-500" />
                        <span className="text-sm font-medium">{uploadedFile.name}</span>
                        <span className="text-xs text-muted-foreground">Click to change file</span>
                      </>
                    ) : (
                      <>
                        <Upload className="w-8 h-8 text-muted-foreground" />
                        <span className="text-sm font-medium">Click to upload or drag and drop</span>
                        <span className="text-xs text-muted-foreground">
                          Supports .txt, .md, .json files
                        </span>
                      </>
                    )}
                  </label>
                </div>
              </div>

              {/* Document Selection */}
              {documents.length > 0 && (
                <div className="space-y-2">
                  <label className="text-sm font-medium">Your Documents</label>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                    {documents.slice(0, 4).map(doc => (
                      <button
                        key={doc.documentId}
                        onClick={() => handleSelectDocument(doc.documentId)}
                        className={`p-3 border rounded-lg text-left transition-all hover:bg-accent ${
                          selectedDocument === doc.documentId ? 'border-primary bg-accent' : 'border-border'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <FileText className="w-4 h-4 text-muted-foreground" />
                          <span className="text-sm font-medium truncate">{doc.fileName}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Or Divider */}
              <div className="flex items-center gap-4">
                <div className="flex-1 h-px bg-border" />
                <span className="text-sm text-muted-foreground">OR</span>
                <div className="flex-1 h-px bg-border" />
              </div>

              {/* Manual Input */}
              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Quiz Title</label>
                  <Input
                    placeholder="e.g., Introduction to React"
                    value={quizTitle}
                    onChange={(e) => setQuizTitle(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Document Text</label>
                  <Textarea
                    placeholder="Paste your document text here..."
                    value={documentText}
                    onChange={(e) => setDocumentText(e.target.value)}
                    className="min-h-[200px] font-mono text-sm"
                  />
                </div>
              </div>
            </CardContent>
            <CardFooter>
              <Button
                onClick={handleCreateQuiz}
                disabled={isLoading || !quizTitle.trim() || !documentText.trim()}
                className="w-full gap-2"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Generating Quiz...
                  </>
                ) : (
                  <>
                    <Brain className="w-4 h-4" />
                    Generate Quiz
                  </>
                )}
              </Button>
            </CardFooter>
          </Card>
        </div>
      </div>
    );
  }

  // Quiz Taking View
  if (view === 'quiz' && quiz && currentQuestion) {
    // Safety check - ensure currentQuestion has valid data
    if (!currentQuestion.options || currentQuestion.options.length === 0) {
      return (
        <div className="flex flex-col h-full w-full items-center justify-center p-6">
          <Card className="max-w-md">
            <CardHeader>
              <CardTitle>Quiz Data Error</CardTitle>
            </CardHeader>
            <CardContent>
              <p>The quiz questions are not properly formatted. Please try generating a new quiz.</p>
            </CardContent>
            <CardFooter>
              <Button onClick={handleStartNew} className="w-full">
                Create New Quiz
              </Button>
            </CardFooter>
          </Card>
        </div>
      );
    }

    return (
      <div className="flex flex-col h-full w-full overflow-y-auto p-6">
        <div className="max-w-3xl mx-auto w-full space-y-6">
          {/* Header */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <h1 className="text-2xl font-bold">{quiz.title}</h1>
              <Badge variant="outline">
                Question {currentQuestionIndex + 1} of {quiz.questions.length}
              </Badge>
            </div>
            <Progress value={progress} className="h-2" />
          </div>

          {/* Question Card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-xl">
                {currentQuestion.questionText}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {currentQuestion.options?.map((option, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSelectAnswer(currentQuestion.id, option)}
                  className={`w-full p-4 text-left border rounded-lg transition-all ${
                    userAnswers[currentQuestion.id] === option
                      ? 'border-primary bg-primary/10'
                      : 'border-border hover:bg-accent'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center ${
                      userAnswers[currentQuestion.id] === option
                        ? 'border-primary bg-primary'
                        : 'border-muted-foreground'
                    }`}>
                      {userAnswers[currentQuestion.id] === option && (
                        <CheckCircle2 className="w-4 h-4 text-primary-foreground" />
                      )}
                    </div>
                    <span className="flex-1">{option}</span>
                  </div>
                </button>
              ))}
            </CardContent>
            <CardFooter className="flex justify-between">
              <Button
                variant="outlined"
                onClick={handlePrevious}
                disabled={currentQuestionIndex === 0}
              >
                Previous
              </Button>

              {currentQuestionIndex < quiz.questions.length - 1 ? (
                <Button onClick={handleNext} className="gap-2">
                  Next
                  <ChevronRight className="w-4 h-4" />
                </Button>
              ) : (
                <Button
                  onClick={handleSubmitQuiz}
                  disabled={!allQuestionsAnswered || isSubmitting}
                  className="gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Submitting...
                    </>
                  ) : (
                    <>
                      <Trophy className="w-4 h-4" />
                      Submit Quiz
                    </>
                  )}
                </Button>
              )}
            </CardFooter>
          </Card>

          {/* Question Navigator */}
          <div className="flex flex-wrap gap-2">
            {quiz.questions.map((q, idx) => (
              <button
                key={q.id}
                onClick={() => setCurrentQuestionIndex(idx)}
                className={`w-10 h-10 rounded-lg border text-sm font-medium transition-all ${
                  idx === currentQuestionIndex
                    ? 'border-primary bg-primary text-primary-foreground'
                    : userAnswers[q.id]
                    ? 'border-green-500 bg-green-500/10 text-green-500'
                    : 'border-border hover:bg-accent'
                }`}
              >
                {idx + 1}
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Results View
  if (view === 'result' && result) {
    const scorePercentage = result.score;
    const isPassed = scorePercentage >= 70;

    return (
      <div className="flex flex-col h-full w-full overflow-y-auto p-6">
        <div className="max-w-4xl mx-auto w-full space-y-6">
          {/* Score Card */}
          <Card className="border-primary">
            <CardHeader className="text-center pb-4">
              <div className="mx-auto mb-4">
                <Trophy className={`w-16 h-16 ${isPassed ? 'text-yellow-500' : 'text-muted-foreground'}`} />
              </div>
              <CardTitle className="text-3xl">Quiz Completed!</CardTitle>
              <CardDescription className="text-lg">
                You scored {result.correctAnswers} out of {result.totalQuestions} questions correctly
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="text-center">
                <div className="text-5xl font-bold text-primary mb-2">
                  {scorePercentage.toFixed(0)}%
                </div>
                <Progress value={scorePercentage} className="h-3" />
              </div>
              {isPassed ? (
                <Badge variant="success" className="w-full justify-center py-2 text-base">
                  Passed! Great job! 🎉
                </Badge>
              ) : (
                <Badge variant="warning" className="w-full justify-center py-2 text-base">
                  Keep practicing! You can do better! 💪
                </Badge>
              )}
            </CardContent>
          </Card>

          {/* Feedback */}
          <div className="space-y-4">
            <h2 className="text-xl font-bold">Answer Review</h2>
            {result.feedback.map((fb, idx) => (
              <Card key={fb.questionId} className={fb.isCorrect ? 'border-green-500' : 'border-red-500'}>
                <CardHeader>
                  <div className="flex items-start gap-3">
                    {fb.isCorrect ? (
                      <CheckCircle2 className="w-6 h-6 text-green-500 mt-1" />
                    ) : (
                      <XCircle className="w-6 h-6 text-red-500 mt-1" />
                    )}
                    <div className="flex-1">
                      <CardTitle className="text-lg mb-2">
                        Question {idx + 1}
                      </CardTitle>
                      {!fb.isCorrect && (
                        <div className="space-y-2 text-sm">
                          <div>
                            <span className="font-medium text-red-500">Your answer: </span>
                            <span>{fb.yourAnswer || 'Not answered'}</span>
                          </div>
                          <div>
                            <span className="font-medium text-green-500">Correct answer: </span>
                            <span>{fb.correctAnswer}</span>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </CardHeader>
                {!fb.isCorrect && fb.explanation && (
                  <CardContent>
                    <div className="bg-muted p-4 rounded-lg">
                      <p className="text-sm font-medium mb-2">Explanation:</p>
                      <p className="text-sm text-muted-foreground">{fb.explanation}</p>
                    </div>
                  </CardContent>
                )}
              </Card>
            ))}
          </div>

          {/* Actions */}
          <div className="flex gap-3">
            <Button onClick={handleStartNew} className="flex-1 gap-2">
              <RefreshCw className="w-4 h-4" />
              Create New Quiz
            </Button>
            <Button onClick={() => setView('history')} variant="outlined" className="flex-1 gap-2">
              <History className="w-4 h-4" />
              View History
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // History View
  if (view === 'history') {
    return (
      <div className="flex flex-col h-full w-full overflow-y-auto p-6">
        <div className="max-w-4xl mx-auto w-full space-y-6">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold flex items-center gap-2">
              <History className="w-6 h-6" />
              Quiz History
            </h1>
            <Button onClick={handleStartNew} className="gap-2">
              <Brain className="w-4 h-4" />
              New Quiz
            </Button>
          </div>

          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
            </div>
          ) : submissions.length === 0 ? (
            <Card>
              <CardContent className="text-center py-12">
                <BookOpen className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                <p className="text-muted-foreground">No quiz attempts yet</p>
                <Button onClick={handleStartNew} className="mt-4 gap-2">
                  <Brain className="w-4 h-4" />
                  Take Your First Quiz
                </Button>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-4">
              {submissions.map((submission) => (
                <Card key={submission.id}>
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <div>
                        <CardTitle>{submission.quiz.title}</CardTitle>
                        <CardDescription>
                          {submission.quiz.questions.length} questions
                        </CardDescription>
                      </div>
                      <Badge variant={submission.score >= 70 ? 'success' : 'warning'}>
                        {submission.score}%
                      </Badge>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <Progress value={submission.score} className="h-2" />
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  return null;
};

