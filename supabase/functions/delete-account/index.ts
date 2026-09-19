import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (request) => {
  if (request.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const authorization = request.headers.get("Authorization");
  if (!authorization) {
    return new Response("Missing authorization", { status: 401 });
  }

  const userClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authorization } } },
  );
  const { data: { user }, error: userError } = await userClient.auth.getUser();
  if (userError || !user) {
    return new Response("Unauthorized", { status: 401 });
  }

  const adminClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const collectUserObjects = async (prefix: string): Promise<string[]> => {
    const { data, error } = await adminClient.storage
      .from("hushkeep-private")
      .list(prefix, { limit: 1000, offset: 0 });
    if (error) throw error;

    const paths: string[] = [];
    for (const entry of data ?? []) {
      const path = `${prefix}/${entry.name}`;
      if (entry.id === null) {
        paths.push(...await collectUserObjects(path));
      } else {
        paths.push(path);
      }
    }
    return paths;
  };

  const userObjects = await collectUserObjects(user.id);
  for (let index = 0; index < userObjects.length; index += 100) {
    const batch = userObjects.slice(index, index + 100);
    const { error: storageError } = await adminClient.storage
      .from("hushkeep-private")
      .remove(batch);
    if (storageError) {
      return new Response(JSON.stringify({ error: storageError.message }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    }
  }

  const { error } = await adminClient.auth.admin.deleteUser(user.id);
  if (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }

  return new Response(JSON.stringify({ deleted: true }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});
