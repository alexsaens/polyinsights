

# **Product Requirements Document (PRD): Market Insight & Report Generator**

## **1\. Executive Summary**

A web-based intelligence tool designed to answer complex market questions using real-time data from Polymarket. The system accepts natural language queries, analyzes key themes, aggregates betting market data (volume, probabilities), and generates comprehensive reports. The goal is to provide deep, actionable insights for professionals through a high-engagement, "flashy" interface.

## **2\. Target Audience (Personas)**

* **The Analyst:** Needs raw data validations and quick sentiment checks.  
* **The Generalized Expert:** Seeks broad market consensus on niche topics.  
* **The Marketing Director:** Looking for trends and consumer sentiment to inform strategy.

## **3\. Technical Stack**

* **Front-End:** Next.js (React)  
* **Middleware/Workflow:** n8n  
* **Database:** Supabase  
* **Data Source:** Polymarket API

## **4\. Functional Requirements**

### **4.1. Front-End (Next.js)**

* **Visual Style:** High-impact, "flashy" marketing-style aesthetic. Visually engaging and modern.  
* **Input Interface:** Centralized search bar inviting the user to input a natural language question (e.g., *"Will shipping be impacted by new US-based tariffs?"*).  
* **Display:**  
  * **Intermediate Stage:** Display a 200–300 word summary of findings.  
  * **Final Output:** On-screen rendering of the full report with a "Download as PDF" button.

### **4.2. Middleware & Logic (n8n)**

* **Keyword Analysis:**  
  * Ingest the user's natural language question.  
  * Perform keyword extraction to identify core entities (e.g., Extract "Tariffs" and "Shipping" from the user query).  
* **Data Retrieval:**  
  * Query the Polymarket API based on extracted keywords.  
  * Retrieve key metrics: Market Volume, Yes/No Ratios (Odds), and overall sentiment.  
* **Summary Generation:**  
  * Synthesize retrieved data into a short (200-300 word) summary.  
* **Report Generation:**  
  * Upon user confirmation, generate a comprehensive, detailed report.

### **4.3. Database (Supabase)**

* Store user queries, generated summaries, and final reports.  
* Maintain logs for KPI tracking (query sophistication and volume).

## **5\. User Flow**

1. **Search:** User lands on the homepage and enters a question (e.g., *"What are the trends for shoes in Cambodia?"*).  
2. **Processing:** System extracts keywords and queries Polymarket.  
3. **Review (The Loop):**  
   * System presents a 200-300 word summary.  
   * **User Action:** User reviews the summary.  
     * *Option A:* **Refine:** User provides feedback ("No, I'm looking for X"). System iterates.  
     * *Option B:* **Approve:** User clicks "Move Forward."  
4. **Final Output:** System generates the comprehensive report. User views it on-screen or downloads it as a PDF.

## **6\. Success Metrics (KPIs)**

* **Query Volume:** Total number of queries processed per day.  
* **Query Sophistication (Quality Score):**  
  * *Low Score:* Simple keyword searches (e.g., "Shoes," "Bikes").  
  * *High Score:* Complex, natural language queries (e.g., *"Will shipping be impacted by new US-based tariff approaches?"*).

## **7\. Timeline**

* **Target:** 3 Weeks to MVP.  
* **Milestones:** (To be defined in Workback Schedule).

---

### **Would you like me to proceed with generating the three specific coding prompts (n8n, Supabase, Front-end) for Claude now?**

