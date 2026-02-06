Here is the comprehensive **3-Week Workback Schedule** to get the "Market Insight & Report Generator" to MVP.

This schedule prioritizes the "Logic First" approach—ensuring the n8n pipeline and data retrieval work before building the flashy UI on top of it.

### **Phase 1: The "Brain" & Infrastructure (Week 1\)**

**Goal:** Have a functional n8n pipeline that can take a question, fetch Polymarket data, and return a text summary.

* **Days 1-2: Database & API Setup**  
  * \[ \] **Supabase:** Run the SQL prompt to set up queries, summaries, and reports tables.  
  * \[ \] **Polymarket API:** Get API keys (if needed) and test endpoints using Postman/Curl to understand the JSON structure (Volume, Odds, Sentiment).  
  * \[ \] **Repo Init:** Initialize the Next.js project repository.  
* **Days 3-5: n8n Workflow Construction**  
  * \[ \] **Step 1 (Ingest):** configure n8n Webhook to accept JSON payload { "question": "..." }.  
  * \[ \] **Step 2 (NLP):** Connect LLM node (OpenAI/Anthropic) to extract keywords from the question.  
  * \[ \] **Step 3 (Fetch):** Connect HTTP Request node to query Polymarket using those keywords.  
  * \[ \] **Step 4 (Synthesize):** Feed API results into a second LLM node to generate the 200-word summary.  
* **Milestone 1 (End of Week 1):** You can send a curl request with a question like *"Will Bitcoin hit 100k?"* and receive a clean 200-word summary JSON response from your n8n webhook.

---

### **Phase 2: The "Face" & The Loop (Week 2\)**

**Goal:** Build the "Flashy" UI and connect it to the n8n brain. Enable the "Review/Feedback" loop.

* **Days 6-8: Front-End Core**  
  * \[ \] **Hero Interface:** Build the central search bar with Tailwind CSS (focus on high-impact visuals).  
  * \[ \] **State Management:** Implement the loading states (animations) to handle the 5-10 second processing time.  
  * \[ \] **Integration:** Connect the Search Input to the n8n Webhook (Stage 1).  
* **Days 9-10: The Feedback Loop**  
  * \[ \] **Summary Display:** Create the component to render the returned 200-word summary card.  
  * \[ \] **Feedback UI:** Add "Refine Query" (re-triggers Stage 1\) and "Move Forward" (triggers Stage 2\) buttons.  
  * \[ \] **Refinement Logic:** Ensure that if a user says "No, I meant X," the new prompt is sent back to n8n with the conversation history/context.  
* **Milestone 2 (End of Week 2):** You have a working web page where you can search, see a loading animation, read a summary, and click "Move Forward" (even if the final report isn't perfect yet).

---

### **Phase 3: The "Finish" & Polish (Week 3\)**

**Goal:** Generate the detailed report, enable PDF download, and finalize the "Flashy" aesthetic.

* **Days 11-13: Final Report & PDF**  
  * \[ \] **n8n Stage 2:** Build the "Comprehensive Report" branch in n8n. This prompts the LLM to write a long-form article based on the cached data.  
  * \[ \] **Report UI:** Create the view to display the long-form report on screen.  
  * \[ \] **PDF Generation:** Integrate html2canvas and jspdf to convert the Report UI into a downloadable PDF file.  
* **Days 14-15: KPI Tracking & UI Polish**  
  * \[ \] **Data Logging:** Ensure n8n is writing the "Sophistication Score" and "Query Volume" into Supabase for your KPIs.  
  * \[ \] **Visual Polish:** Add hover effects, transitions, and ensure mobile responsiveness.  
  * \[ \] **Deployment:** Deploy Front-end to Vercel/Netlify.  
* **Milestone 3 (Launch):** Full MVP is live. Users can search, review, generate a report, and download it.

---

### **Critical Path Risks (Watch out for these)**

1. **Polymarket Data Quality:** Sometimes niche queries returns 0 results.  
   * *Mitigation:* Add a fallback in n8n where the LLM says "Market data is thin, but here is general web context..." if Polymarket returns null.  
2. **Latency:** The "Chain of Thought" (Search \-\> NLP \-\> API \-\> Summary) might take 10+ seconds.  
   * *Mitigation:* Ensure your "Loading" animation in Week 2 is very engaging to keep the user from refreshing the page.

### **Next Step**

You mentioned "that's all I have right now," so this concludes the planning package.

**Would you like me to wrap all of this (The PRD, The Prompts, and this Schedule) into a single artifact/text block so you can copy-paste it all at once?**

