import { createClient } from "npm:@supabase/supabase-js@2";
import { AccessToken } from "npm:livekit-server-sdk@2";

const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type" };
Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const authHeader = request.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) return Response.json({ error: "unauthorized" }, { status: 401, headers: cors });
    const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: authHeader } } });
    const { data: { user }, error } = await supabase.auth.getUser();
    if (error || !user) return Response.json({ error: "unauthorized" }, { status: 401, headers: cors });
    const { room, canPublish = true } = await request.json();
    if (typeof room !== "string" || !/^[a-zA-Z0-9_-]{3,80}$/.test(room)) return Response.json({ error: "invalid room" }, { status: 400, headers: cors });
    const apiKey = Deno.env.get("LIVEKIT_API_KEY");
    const apiSecret = Deno.env.get("LIVEKIT_API_SECRET");
    if (!apiKey || !apiSecret) return Response.json({ error: "LiveKit secrets are not configured" }, { status: 503, headers: cors });
    const token = new AccessToken(apiKey, apiSecret, { identity: user.id, name: user.user_metadata?.display_name ?? "VYRO User", ttl: "1h" });
    token.addGrant({ roomJoin: true, room, canPublish, canSubscribe: true, canPublishData: true });
    return Response.json({ token: await token.toJwt(), url: Deno.env.get("LIVEKIT_URL") }, { headers: { ...cors, "Content-Type": "application/json" } });
  } catch (e) { return Response.json({ error: e instanceof Error ? e.message : "server error" }, { status: 500, headers: cors }); }
});
