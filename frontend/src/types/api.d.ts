export interface StudentAnswer {
  score: number | null;
  feedback: string | null;
  evaluatedAt: string | null;
  evaluationDetails?: EvaluationDetails;
  weaknessTags?: string;
}

export interface EvaluationDetails {
  // ... existing code ...
} 