# Edge Functions

## livekit-token

هذه الدالة تُصدر LiveKit JWT مؤقتًا بعد التحقق من Supabase Auth. انشرها من مجلد المشروع:

```bash
supabase functions deploy livekit-token
```

تحتاج الأسرار التالية في Supabase Edge Function Secrets:

```text
LIVEKIT_URL
LIVEKIT_API_KEY
LIVEKIT_API_SECRET
```

لا تضع `LIVEKIT_API_SECRET` في Android أو GitHub. بعد النشر نفّذ `supabase/migrations/002_rooms.sql` في SQL Editor. يجب أن يكون المستخدم مسجلًا في VYRO حتى يحصل على Token.