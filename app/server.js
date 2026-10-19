// Bukhari Mosque Backend Server & API Gateway (Port 3000)
const http = require('http');
const fs = require('fs');
const url = require('url');

const PORT = 3000;

// In-memory data store matching Bukhari schema
let courses = [
  {
    id: "course-summer-1447",
    name: "دورة الصيف 1447هـ - متون العقيدة والتوحيد",
    startDate: "2026-09-01",
    endDate: "2026-10-30",
    status: "active",
    isDefault: true,
    academicSeason: "1447هـ / 2026م",
    createdAt: "2026-09-01T00:00:00.000Z"
  }
];

let committees = [
  {
    id: "comm-1",
    committeeNumber: 1,
    committeeName: "لجنة الإمام الشاطبي",
    associationName: "مسجد الإمام البخاري للعلوم الشرعية",
    branch: "فرع المتقدمين",
    competitionBook: "مسابقة كتاب الأصول الثلاثة",
    courseId: "course-summer-1447",
    courseName: "دورة الصيف 1447هـ - متون العقيدة والتوحيد",
    sheikhName: "الشيخ د. عبد الرحمن بن منصور",
    sheikhPhone: "01012345678",
    sheikhCountry: "مصر - القاهرة",
    studentsCount: 25,
    isWomenCommittee: false,
    createdDate: "2026-09-01",
    createdAtTime: "08:30 ص",
    updatedAt: "2026-10-16T12:00:00.000Z",
    stages: {
      registration: { completed: true, completedAt: "2026-10-16T08:30:00.000Z" },
      exam: { completed: true, completedAt: "2026-10-16T09:15:00.000Z" },
      sorting: { completed: true, completedAt: "2026-10-16T10:00:00.000Z" },
      scoring: { completed: false, completedAt: null },
      aggregation: { completed: false, completedAt: null },
      announcement: { completed: false, completedAt: null },
      distribution: { completed: false, completedAt: null },
      ceremony: { completed: false, completedAt: null },
      honoring: { completed: false, completedAt: null }
    },
    scoringDetails: {
      examiner1: "الشيخ أحمد",
      examiner2: "الشيخ مصطفى",
      questions: [
        { id: "q1", number: 1, text: "السؤال الأول في الأصول الثلاثة", maxMarks: 20, completed: true, completedAt: "2026-10-16T10:30:00.000Z" },
        { id: "q2", number: 2, text: "السؤال الثاني في شروط الصلاة", maxMarks: 20, completed: false, completedAt: null }
      ]
    },
    aggregationDetails: {
      aggregatorName: "أ. محمود شاكر",
      isConfirmedMatch: false
    },
    ceremonyDetails: {
      date: "2026-10-25",
      time: "06:30 م",
      venue: "المصلى الكبير - مسجد الإمام البخاري"
    }
  },
  {
    id: "comm-2",
    committeeNumber: 2,
    committeeName: "لجنة أم المؤمنين عائشة (أخوات)",
    associationName: "مسجد الإمام البخاري للعلوم الشرعية",
    branch: "فرع النساء والفتيات",
    competitionBook: "مسابقة الأربعين النووية",
    courseId: "course-summer-1447",
    courseName: "دورة الصيف 1447هـ - متون العقيدة والتوحيد",
    sheikhName: "الشيخة أم أنس المصرية",
    sheikhPhone: "01098765432",
    sheikhCountry: "مصر - الجيزة",
    studentsCount: 30,
    isWomenCommittee: true,
    createdDate: "2026-09-02",
    createdAtTime: "09:00 ص",
    updatedAt: "2026-10-16T12:00:00.000Z",
    stages: {
      registration: { completed: true, completedAt: "2026-10-16T09:00:00.000Z" },
      exam: { completed: false, completedAt: null },
      sorting: { completed: false, completedAt: null },
      scoring: { completed: false, completedAt: null },
      aggregation: { completed: false, completedAt: null },
      announcement: { completed: false, completedAt: null },
      distribution: { completed: false, completedAt: null },
      ceremony: { completed: false, completedAt: null },
      honoring: { completed: false, completedAt: null }
    },
    scoringDetails: { questions: [] },
    aggregationDetails: { isConfirmedMatch: false },
    ceremonyDetails: {}
  }
];

let users = [
  {
    id: "admin-1",
    name: "المشرف العام (إدارة المنظومة)",
    phone: "01000000000",
    email: "admin@bukhari.org",
    roleId: "super_admin",
    roleKey: "super_admin",
    status: "approved",
    permissions: ["all"],
    createdAt: "2026-09-01T00:00:00.000Z",
    updatedAt: "2026-10-16T12:00:00.000Z"
  },
  {
    id: "user-assistant-1",
    name: "م. محمد فوزي (معاون الميدان)",
    phone: "01011111111",
    email: "assistant@bukhari.org",
    roleId: "assistant",
    roleKey: "assistant",
    status: "approved",
    assignedCommitteeIds: ["comm-1", "comm-2"],
    permissions: ["manage_committees", "calendar_schedule"],
    createdAt: "2026-09-05T00:00:00.000Z",
    updatedAt: "2026-10-16T12:00:00.000Z"
  }
];

let tasks = [
  {
    id: "task-1",
    title: "متابعة تسليم كشوفات درجات لجنة الشاطبي",
    description: "استلام كشف درجات كتاب الأصول الثلاثة والتحقق من التوقيعات",
    date: "2026-10-17",
    time: "10:00 ص",
    priority: "urgent",
    type: "deliverable",
    completed: false,
    completedAt: null,
    committeeId: "comm-1",
    committeeNumber: 1,
    assignedAssistantName: "م. محمد فوزي",
    deliverable: "كشف درجات معتمد وموقع"
  }
];

let exams = [
  {
    id: "exam-1",
    title: "اختبار كتاب الأصول الثلاثة وأدلتها",
    book: "الأصول الثلاثة",
    branch: "فرع المتقدمين",
    totalMarks: 100,
    durationMinutes: 60,
    questionsCount: 5,
    pdfFileName: "نموذج_الأصول_الثلاثة.pdf",
    pdfFileSize: "1.2 MB",
    uploadedAt: "2026-10-10T08:00:00.000Z"
  }
];

// Helper to send JSON responses
function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Expected-Updated-At, Idempotency-Key, X-App-Version-Code'
  });
  res.end(JSON.stringify(data));
}

// Generate minimal valid PDF buffer for testing
function generateSamplePdf() {
  const content = `%PDF-1.4
1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj
4 0 obj << /Length 55 >> stream
BT /F1 24 Tf 100 700 Td (Imam Bukhari Mosque Exam PDF) ET
endstream
endobj
xref
0 5
0000000000 65535 f 
0000000010 00000 n 
0000000060 00000 n 
0000000117 00000 n 
0000000204 00000 n 
trailer << /Size 5 /Root 1 0 R >>
startxref
309
%%EOF`;
  return Buffer.from(content, 'utf-8');
}

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const path = parsedUrl.pathname;
  const method = req.method;

  // CORS Preflight
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Expected-Updated-At, Idempotency-Key, X-App-Version-Code'
    });
    return res.end();
  }

  // Collect request body
  let body = '';
  req.on('data', chunk => { body += chunk.toString(); });
  req.on('end', () => {
    let jsonBody = {};
    if (body) {
      try { jsonBody = JSON.parse(body); } catch (e) {}
    }

    // --- API ROUTES ---

    // 1. Auth: Login
    if (path === '/api/auth/login' && method === 'POST') {
      const { identifier, password } = jsonBody;
      const user = users.find(u => (u.phone === identifier || u.email === identifier || identifier === 'admin'));
      if (user || (identifier === 'admin' && password === '504')) {
        const found = user || users[0];
        return sendJson(res, 200, {
          success: true,
          token: "jwt_token_bukhari_" + Date.now(),
          refreshToken: "jwt_refresh_" + Date.now(),
          user: found,
          mustChangePassword: false,
          message: "تم تسجيل الدخول بنجاح"
        });
      }
      return sendJson(res, 401, { success: false, message: "بيانات الدخول غير صحيحة" });
    }

    // 2. Auth: Me
    if (path === '/api/auth/me' && method === 'GET') {
      return sendJson(res, 200, {
        success: true,
        user: users[0],
        permissions: {
          role: "super_admin",
          actions: { "all": true, "summerRows.write": true, "audit.read": true, "exams.write": true, "exams.file": true, "tasks.write": true, "users.read": true, "database.admin": true, "stats.read": true },
          committeeScope: "all",
          exams: "all",
          mustChangePassword: false
        }
      });
    }

    // 3. Courses
    if (path === '/api/courses' && method === 'GET') {
      return sendJson(res, 200, { success: true, courses });
    }

    // 4. Committees List
    if (path === '/api/committees' && method === 'GET') {
      return sendJson(res, 200, { success: true, committees, total: committees.length, hasMore: false });
    }

    // 5. Create Committee
    if (path === '/api/committees' && method === 'POST') {
      const newComm = {
        ...jsonBody,
        id: jsonBody.id || "comm-" + Date.now(),
        updatedAt: new Date().toISOString()
      };
      committees.push(newComm);
      return sendJson(res, 200, { success: true, committee: newComm });
    }

    // 6. Committee Actions (Stages, Reception, etc.)
    if (path.startsWith('/api/committees/') && path.endsWith('/actions') && method === 'POST') {
      const parts = path.split('/');
      const commId = decodeURIComponent(parts[3]);
      const comm = committees.find(c => c.id === commId);
      if (comm) {
        comm.updatedAt = new Date().toISOString();
        const act = jsonBody.action || {};
        if (act.type === 'completeStage' && act.stage) {
          if (!comm.stages[act.stage]) comm.stages[act.stage] = {};
          comm.stages[act.stage].completed = true;
          comm.stages[act.stage].completedAt = new Date().toISOString();
        }
        return sendJson(res, 200, { success: true, committee: comm });
      }
      return sendJson(res, 404, { success: false, message: "اللجنة غير موجودة" });
    }

    // 7. Update Committee
    if (path.startsWith('/api/committees/') && (method === 'PATCH' || method === 'PUT')) {
      const commId = decodeURIComponent(path.split('/')[3]);
      const expectedUpdated = req.headers['x-expected-updated-at'];
      const comm = committees.find(c => c.id === commId);
      if (comm) {
        if (expectedUpdated && comm.updatedAt && expectedUpdated !== comm.updatedAt) {
          return sendJson(res, 409, { success: false, message: "حدث تعارض: تم تعديل بيانات هذه اللجنة مسبقاً من مكان آخر" });
        }
        Object.assign(comm, jsonBody);
        comm.updatedAt = new Date().toISOString();
        return sendJson(res, 200, { success: true, committee: comm });
      }
      return sendJson(res, 404, { success: false, message: "اللجنة غير موجودة" });
    }

    // 8. Tasks
    if (path === '/api/tasks' && method === 'GET') {
      return sendJson(res, 200, { success: true, tasks, total: tasks.length, hasMore: false });
    }
    if (path.startsWith('/api/tasks/') && path.endsWith('/toggle') && method === 'PATCH') {
      const taskId = decodeURIComponent(path.split('/')[3]);
      const t = tasks.find(x => x.id === taskId);
      if (t) {
        t.completed = jsonBody.completed !== undefined ? jsonBody.completed : !t.completed;
        t.completedAt = t.completed ? new Date().toISOString() : null;
        return sendJson(res, 200, { success: true, task: t });
      }
      return sendJson(res, 404, { success: false, message: "المهمة غير موجودة" });
    }

    // 9. Exams & Exam PDF download
    if (path === '/api/exams' && method === 'GET') {
      return sendJson(res, 200, { success: true, exams });
    }
    if (path.startsWith('/api/exams/') && path.endsWith('/file') && method === 'GET') {
      const pdfBuffer = generateSamplePdf();
      res.writeHead(200, {
        'Content-Type': 'application/pdf',
        'Content-Disposition': 'inline; filename="exam.pdf"',
        'Content-Length': pdfBuffer.length
      });
      return res.end(pdfBuffer);
    }

    // 10. Users
    if (path === '/api/users' && method === 'GET') {
      return sendJson(res, 200, { success: true, users });
    }
    if (path === '/api/users/assignable' && method === 'GET') {
      return sendJson(res, 200, {
        success: true,
        users: users.map(u => ({ id: u.id, name: u.name, role: u.roleKey }))
      });
    }

    // 11. Database Status
    if (path === '/api/database/status' && method === 'GET') {
      return sendJson(res, 200, {
        database: "bukhari_competition_db",
        postgresVersion: "16.4 (Debian)",
        latencyMs: 14,
        tls: "TLSv1.3",
        migrations: { total: 5, pending: [] },
        tables: [
          { name: "committees", count: committees.length },
          { name: "courses", count: courses.length },
          { name: "exams", count: exams.length },
          { name: "tasks", count: tasks.length },
          { name: "app_users", count: users.length }
        ]
      });
    }

    // 12. Web Dashboard (when visiting http://localhost:3000/ directly in browser)
    if (path === '/' || path === '/index.html') {
      const html = `<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>خادم مسجد الإمام البخاري للعلوم الشرعية</title>
  <style>
    body { font-family: system-ui, -apple-system, sans-serif; background: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
    .card { background: #1E293B; border-radius: 16px; padding: 24px; margin-bottom: 20px; border: 1px solid #334155; }
    .badge { background: #10B981; color: #064E3B; font-weight: bold; padding: 4px 12px; border-radius: 999px; font-size: 14px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
    .stat { background: #0F172A; padding: 16px; border-radius: 12px; border: 1px solid #334155; }
    h1 { color: #38BDF8; font-size: 26px; }
    h3 { margin: 0 0 8px 0; color: #94A3B8; font-size: 14px; }
    .number { font-size: 28px; font-weight: bold; color: #F8FAFC; }
  </style>
</head>
<body>
  <div class="card">
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <h1>🕌 خادم منظومة مسجد الإمام البخاري</h1>
      <span class="badge">🟢 السيرفر يعمل بنجاح (Port 3000)</span>
    </div>
    <p style="color: #94A3B8;">الخادم المحلي المشترك مع تطبيق الأندرويد متصل ويعمل بجاهزية تامة عبر النفق السحابي.</p>
  </div>

  <div class="grid">
    <div class="stat"><h3>اللجان الميدانية</h3><div class="number">${committees.length}</div></div>
    <div class="stat"><h3>الدورات المعتمدة</h3><div class="number">${courses.length}</div></div>
    <div class="stat"><h3>الامتحانات والملفات</h3><div class="number">${exams.length}</div></div>
    <div class="stat"><h3>المهام والجدول</h3><div class="number">${tasks.length}</div></div>
  </div>
</body>
</html>`;
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return res.end(html);
    }

    // Default 404 for unknown endpoints
    return sendJson(res, 404, { success: false, message: "المسار غير موجود في خادم المنظومة" });
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Bukhari Server is listening on http://0.0.0.0:${PORT}`);
});
