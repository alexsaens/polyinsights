# PolyInsights n8n Workflow (Stage 1 + Stage 2)

This blueprint matches your PRD flow and Supabase schema in `supabase/schema.sql`.

## Assumptions
- n8n has access to:
  - `OPENAI_API_KEY`
  - `SUPABASE_URL`
  - `SUPABASE_SERVICE_ROLE_KEY`
- We use two webhooks in one workflow:
  - `POST /polymarket/analyze` (Stage 1)
  - `POST /polymarket/report` (Stage 2)
- `market_data.raw_payload` is stored for each fetched market row.

## Stage 1: Analysis Loop (`POST /polymarket/analyze`)

### Input body
```json
{
  "question": "Will shipping be impacted by new US-based tariffs?"
}
```

### Nodes (in order)
1. **Webhook: Analyze**
   - Method: `POST`
   - Path: `polymarket/analyze`
   - Response mode: Using `Respond to Webhook` node

2. **Code: Validate Input**
   - Fail if `question` missing/empty.
   - Trim string and pass forward.

3. **OpenAI Chat: Keyword + Sophistication Extractor**
   - Model: `gpt-4o-mini` (or equivalent)
   - Output format: strict JSON (see prompt below)

4. **Code: Parse Extractor JSON**
   - Parse assistant output into:
     - `keywords` (array)
     - `keywords_query` (comma-joined)
     - `sophistication_label` (`low|high`)
     - `sophistication_score` (`0-100`)

5. **HTTP Request: Insert Query (Supabase)**
   - `POST {{$env.SUPABASE_URL}}/rest/v1/queries`
   - Headers:
     - `apikey: {{$env.SUPABASE_SERVICE_ROLE_KEY}}`
     - `Authorization: Bearer {{$env.SUPABASE_SERVICE_ROLE_KEY}}`
     - `Content-Type: application/json`
     - `Prefer: return=representation`
   - Body:
     - `user_id`: from your authenticated frontend token context (recommended), or temporary backend-mapped user id
     - `question`
     - `sophistication_label`
     - `sophistication_score`
     - `status: "analysis"`

6. **HTTP Request: Polymarket Search**
   - Method: `GET`
   - URL (example): `https://gamma-api.polymarket.com/markets`
   - Query params:
     - `search={{$json.keywords_query}}`
     - `limit=20`
     - `active=true`
   - Keep full response JSON for raw payload storage.

7. **Code: Normalize Market Rows**
   - Map API markets to row payloads for `market_data`:
     - `query_id`
     - `market_id`
     - `market_title`
     - `volume_numeric`
     - `yes_probability`
     - `no_probability`
     - `sentiment` (derived rule: yes/no spread + confidence bucket)
     - `raw_payload` (entire market object)

8. **HTTP Request: Insert Market Data (Supabase bulk)**
   - `POST {{$env.SUPABASE_URL}}/rest/v1/market_data`
   - Same auth headers as above
   - Body: array of mapped rows

9. **OpenAI Chat: Summary Generator (200-300 words)**
   - Inputs: original question + normalized market rows
   - Output: plain markdown summary

10. **HTTP Request: Upsert Summary**
    - `POST {{$env.SUPABASE_URL}}/rest/v1/summaries`
    - Headers include:
      - `Prefer: resolution=merge-duplicates,return=representation`
    - Body:
      - `query_id`
      - `summary_text`
      - `model_name`

11. **HTTP Request: Update Query Status**
    - `PATCH {{$env.SUPABASE_URL}}/rest/v1/queries?id=eq.{{$json.query_id}}`
    - Body: `{ "status": "review" }`

12. **Respond to Webhook: Stage 1 Response**
    - Return:
      - `session_id` (use `query_id`)
      - `summary`
      - `sophistication_label`
      - `sophistication_score`
      - `market_count`

### Stage 1 response
```json
{
  "session_id": "uuid",
  "summary": "200-300 word summary...",
  "sophistication_label": "high",
  "sophistication_score": 84,
  "market_count": 12
}
```

## Stage 2: Final Report (`POST /polymarket/report`)

### Input body
```json
{
  "session_id": "uuid",
  "action": "move_forward"
}
```

### Nodes (in order)
1. **Webhook: Report**
   - Method: `POST`
   - Path: `polymarket/report`

2. **Code: Validate Report Request**
   - Require `session_id` and `action === "move_forward"`.

3. **HTTP Request: Fetch Query + Summary + Market Data**
   - Pull from Supabase REST:
     - `queries?id=eq.{session_id}`
     - `summaries?query_id=eq.{session_id}`
     - `market_data?query_id=eq.{session_id}`

4. **HTTP Request: Update Query Status**
   - Set `status` to `reporting`.

5. **OpenAI Chat: Report Writer**
   - Input: question + summary + normalized market rows
   - Output: long-form markdown (or HTML if preferred)

6. **HTTP Request: Upsert Report**
   - `POST {{$env.SUPABASE_URL}}/rest/v1/reports`
   - Body:
     - `query_id`
     - `report_format` (`markdown`)
     - `report_content`
     - `status` (`completed`)
   - Prefer merge duplicates.

7. **HTTP Request: Update Query Status**
   - Set `status` to `completed`.

8. **Respond to Webhook: Stage 2 Response**
   - Return:
     - `session_id`
     - `report`
     - `format`

### Stage 2 response
```json
{
  "session_id": "uuid",
  "format": "markdown",
  "report": "# Final report\n..."
}
```

## LLM Prompts

### Prompt: Keyword + Sophistication Extractor
Use as the **system prompt**:

```text
You are an analyst extracting market-search intent from user questions.
Return ONLY valid JSON with this exact shape:
{
  "keywords": ["..."],
  "sophistication_label": "low" | "high",
  "sophistication_score": 0-100,
  "reasoning_short": "max 20 words"
}

Rules:
- Extract 3-8 concise keywords/entities useful for prediction market search.
- sophistication_label is "high" when the question has multiple constraints, causal framing, geography/time context, or sector nuance.
- sophistication_score must align with label (low: 0-59, high: 60-100).
- No markdown, no extra keys, no prose outside JSON.
```

### Prompt: Summary Generator (200-300 words)
Use as the **system prompt**:

```text
You synthesize prediction market data into a concise executive summary.

Requirements:
- Write 200-300 words.
- Mention what the markets imply, confidence level, and notable disagreement.
- Use plain English for a business audience.
- Include 3 short sections with markdown headings:
  1) Market Signal
  2) What Drives It
  3) Risks / Blind Spots
- If market coverage is sparse, explicitly say data is thin and lower confidence.
- Do not fabricate market titles or numbers.
```

### Prompt: Report Writer
Use as the **system prompt**:

```text
You are writing a detailed market insight report from prediction market data.

Output markdown only.
Structure:
1. Executive Summary
2. Market Evidence
3. Scenario Analysis (Base/Bull/Bear)
4. Implications for Decision Makers
5. Monitoring Signals (next 2-6 weeks)
6. Appendix: Referenced Markets

Rules:
- Ground claims in supplied market data.
- Call out uncertainty and data gaps explicitly.
- Be specific, practical, and non-hype.
- Target 900-1400 words.
```

## Frontend contract
- Stage 1 endpoint returns `session_id` + summary for review.
- Stage 2 endpoint takes `session_id` and returns final report.
- The frontend should pass end-user JWT to backend or route via server-side function so inserts use correct `user_id`.
