/*
|--------------------------------------------------------------------------
| SHOP-DZ - bright-worker
|--------------------------------------------------------------------------
| بدون supabase-js
| بدون jose
| يستخدم fetch + Web Crypto فقط
|--------------------------------------------------------------------------
*/

const SUPABASE_URL =
    Deno.env.get("SUPABASE_URL") || "";

const SUPABASE_SECRET_KEYS =
    Deno.env.get("SUPABASE_SECRET_KEYS") || "";

const FIREBASE_SERVICE_ACCOUNT_JSON =
    Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") || "";

const NOTIFICATION_SECRET =
    Deno.env.get("NOTIFICATION_SECRET") || "";

/*
|--------------------------------------------------------------------------
| JSON RESPONSE
|--------------------------------------------------------------------------
*/

function jsonResponse(
    data: unknown,
    status = 200
): Response {

    return new Response(
        JSON.stringify(data, null, 2),
        {
            status,
            headers: {
                "Content-Type":
                    "application/json; charset=utf-8"
            }
        }
    );
}

/*
|--------------------------------------------------------------------------
| SUPABASE SECRET KEY
|--------------------------------------------------------------------------
*/

function getSupabaseSecretKey(): string {

    if (!SUPABASE_SECRET_KEYS) {

        throw new Error(
            "SUPABASE_SECRET_KEYS is missing"
        );
    }

    let parsed: Record<string, string>;

    try {

        parsed =
            JSON.parse(
                SUPABASE_SECRET_KEYS
            );

    } catch {

        throw new Error(
            "SUPABASE_SECRET_KEYS is not valid JSON"
        );
    }

    const key =
        parsed["default"];

    if (!key) {

        throw new Error(
            "Default Supabase secret key not found"
        );
    }

    return key;
}

/*
|--------------------------------------------------------------------------
| BASE64URL
|--------------------------------------------------------------------------
*/

function base64UrlEncode(
    input: Uint8Array
): string {

    let binary = "";

    const chunkSize = 0x8000;

    for (
        let i = 0;
        i < input.length;
        i += chunkSize
    ) {

        binary += String.fromCharCode(
            ...input.subarray(
                i,
                Math.min(
                    i + chunkSize,
                    input.length
                )
            )
        );
    }

    return btoa(binary)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/g, "");
}

/*
|--------------------------------------------------------------------------
| PEM -> DER
|--------------------------------------------------------------------------
*/

function pemToArrayBuffer(
    pem: string
): ArrayBuffer {

    const base64 =
        pem
            .replace(
                /-----BEGIN PRIVATE KEY-----/g,
                ""
            )
            .replace(
                /-----END PRIVATE KEY-----/g,
                ""
            )
            .replace(
                /\s/g,
                ""
            );

    const binary =
        atob(base64);

    const bytes =
        new Uint8Array(
            binary.length
        );

    for (
        let i = 0;
        i < binary.length;
        i++
    ) {

        bytes[i] =
            binary.charCodeAt(i);
    }

    return bytes.buffer;
}

/*
|--------------------------------------------------------------------------
| FIREBASE ACCESS TOKEN
|--------------------------------------------------------------------------
*/

async function getFirebaseAccessToken(): Promise<{
    accessToken: string;
    projectId: string;
}> {

    console.log(
        "STEP 4: Starting Firebase OAuth"
    );

    if (!FIREBASE_SERVICE_ACCOUNT_JSON) {

        throw new Error(
            "FIREBASE_SERVICE_ACCOUNT_JSON secret is missing"
        );
    }

    let serviceAccount: Record<string, unknown>;

    try {

        serviceAccount =
            JSON.parse(
                FIREBASE_SERVICE_ACCOUNT_JSON
            );

    } catch {

        throw new Error(
            "FIREBASE_SERVICE_ACCOUNT_JSON is invalid JSON"
        );
    }

    const projectId =
        String(
            serviceAccount.project_id || ""
        );

    const clientEmail =
        String(
            serviceAccount.client_email || ""
        );

    const privateKeyPem =
        String(
            serviceAccount.private_key || ""
        );

    const tokenUri =
        String(
            serviceAccount.token_uri ||
            "https://oauth2.googleapis.com/token"
        );

    if (!projectId) {

        throw new Error(
            "Firebase project_id is missing"
        );
    }

    if (!clientEmail) {

        throw new Error(
            "Firebase client_email is missing"
        );
    }

    if (!privateKeyPem) {

        throw new Error(
            "Firebase private_key is missing"
        );
    }

    console.log(
        "Firebase project:",
        projectId
    );

    /*
    |--------------------------------------------------------------------------
    | IMPORT RSA PRIVATE KEY
    |--------------------------------------------------------------------------
    */

    const privateKey =
        await crypto.subtle.importKey(
            "pkcs8",
            pemToArrayBuffer(
                privateKeyPem
            ),
            {
                name:
                    "RSASSA-PKCS1-v1_5",
                hash:
                    "SHA-256"
            },
            false,
            ["sign"]
        );

    /*
    |--------------------------------------------------------------------------
    | JWT HEADER
    |--------------------------------------------------------------------------
    */

    const header = {

        alg:
            "RS256",

        typ:
            "JWT"
    };

    /*
    |--------------------------------------------------------------------------
    | JWT CLAIMS
    |--------------------------------------------------------------------------
    */

    const now =
        Math.floor(
            Date.now() / 1000
        );

    const payload = {

        iss:
            clientEmail,

        scope:
            "https://www.googleapis.com/auth/firebase.messaging",

        aud:
            tokenUri,

        iat:
            now,

        exp:
            now + 3600
    };

    const encodedHeader =
        base64UrlEncode(
            new TextEncoder().encode(
                JSON.stringify(header)
            )
        );

    const encodedPayload =
        base64UrlEncode(
            new TextEncoder().encode(
                JSON.stringify(payload)
            )
        );

    const unsignedToken =
        encodedHeader +
        "." +
        encodedPayload;

    /*
    |--------------------------------------------------------------------------
    | SIGN JWT
    |--------------------------------------------------------------------------
    */

    const signature =
        await crypto.subtle.sign(
            {
                name:
                    "RSASSA-PKCS1-v1_5"
            },
            privateKey,
            new TextEncoder().encode(
                unsignedToken
            )
        );

    const encodedSignature =
        base64UrlEncode(
            new Uint8Array(
                signature
            )
        );

    const assertion =
        unsignedToken +
        "." +
        encodedSignature;

    console.log(
        "STEP 4A: Firebase JWT created"
    );

    /*
    |--------------------------------------------------------------------------
    | GOOGLE OAUTH
    |--------------------------------------------------------------------------
    */

    const response =
        await fetch(
            tokenUri,
            {
                method:
                    "POST",

                headers: {

                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body:
                    new URLSearchParams({

                        grant_type:
                            "urn:ietf:params:oauth:grant-type:jwt-bearer",

                        assertion:
                            assertion
                    })
            }
        );

    const responseText =
        await response.text();

    console.log(
        "STEP 4B: Firebase OAuth HTTP:",
        response.status
    );

    if (!response.ok) {

        console.error(
            "Firebase OAuth ERROR:",
            responseText
        );

        throw new Error(
            "Firebase OAuth failed: HTTP " +
            response.status
        );
    }

    let data: Record<string, unknown>;

    try {

        data =
            JSON.parse(
                responseText
            );

    } catch {

        throw new Error(
            "Firebase OAuth returned invalid JSON"
        );
    }

    const accessToken =
        String(
            data.access_token || ""
        );

    if (!accessToken) {

        throw new Error(
            "Firebase access_token not found"
        );
    }

    console.log(
        "STEP 4C: Firebase access token obtained"
    );

    return {

        accessToken:
            accessToken,

        projectId:
            projectId
    };
}

/*
|--------------------------------------------------------------------------
| READ ACTIVE TOKENS
|--------------------------------------------------------------------------
*/

async function getActiveTokens(
    supabaseSecretKey: string
) {

    console.log(
        "STEP 3: Reading push_tokens"
    );

    const url =
        SUPABASE_URL +
        "/rest/v1/push_tokens" +
        "?select=id,token&is_active=eq.true";

    const response =
        await fetch(
            url,
            {
                method:
                    "GET",

                headers: {

                    "apikey":
                        supabaseSecretKey,

                    "Authorization":
                        "Bearer " +
                        supabaseSecretKey,

                    "Accept":
                        "application/json"
                }
            }
        );

    const responseText =
        await response.text();

    console.log(
        "Supabase push_tokens HTTP:",
        response.status
    );

    if (!response.ok) {

        console.error(
            "Supabase push_tokens ERROR:",
            responseText
        );

        throw new Error(
            "push_tokens query failed: HTTP " +
            response.status
        );
    }

    let tokens;

    try {

        tokens =
            JSON.parse(
                responseText
            );

    } catch {

        throw new Error(
            "Supabase returned invalid JSON"
        );
    }

    console.log(
        "Token count:",
        Array.isArray(tokens)
            ? tokens.length
            : 0
    );

    return Array.isArray(tokens)
        ? tokens
        : [];
}

/*
|--------------------------------------------------------------------------
| DISABLE TOKEN
|--------------------------------------------------------------------------
*/

async function disableToken(
    supabaseSecretKey: string,
    id: number | string
) {

    console.log(
        "Disabling invalid token:",
        id
    );

    const url =
        SUPABASE_URL +
        "/rest/v1/push_tokens?id=eq." +
        encodeURIComponent(
            String(id)
        );

    const response =
        await fetch(
            url,
            {
                method:
                    "PATCH",

                headers: {

                    "apikey":
                        supabaseSecretKey,

                    "Authorization":
                        "Bearer " +
                        supabaseSecretKey,

                    "Content-Type":
                        "application/json",

                    "Prefer":
                        "return=minimal"
                },

                body:
                    JSON.stringify({
                        is_active:
                            false
                    })
            }
        );

    if (!response.ok) {

        const text =
            await response.text();

        console.error(
            "Disable token failed:",
            response.status,
            text
        );
    }
}

/*
|--------------------------------------------------------------------------
| SEND FCM
|--------------------------------------------------------------------------
*/

async function sendFCM(
    accessToken: string,
    projectId: string,
    token: string,
    order: Record<string, unknown>
) {

    const orderId =
        order.order_id != null
            ? String(order.order_id)
            : "";

    const orderNumber =
        order.order_number != null
            ? String(order.order_number)
            : "";

    const customerName =
        order.customer_name != null
            ? String(order.customer_name)
            : "";

    const total =
        order.total != null
            ? String(order.total)
            : "";

    let body =
        "لديك طلب جديد في SHOP-DZ";

    if (orderNumber) {

        body +=
            " #" +
            orderNumber;
    }

    if (customerName) {

        body +=
            "\nالعميل: " +
            customerName;
    }

    if (total) {

        body +=
            "\nالمبلغ: " +
            total +
            " دج";
    }

    const url =
        "https://fcm.googleapis.com/v1/projects/" +
        encodeURIComponent(projectId) +
        "/messages:send";

    const payload = {

        message: {

            token:
                token,

            notification: {

                title:
                    "🔔 طلب جديد",

                body:
                    body
            },

            data: {

                type:
                    "new_order",

                order_id:
                    orderId,

                order_number:
                    orderNumber,

                customer_name:
                    customerName,

                total:
                    total
            },

            android: {

                priority:
                    "HIGH",

                notification: {

                    sound:
                        "default"
                }
            }
        }
    };

    const response =
        await fetch(
            url,
            {
                method:
                    "POST",

                headers: {

                    "Authorization":
                        "Bearer " +
                        accessToken,

                    "Content-Type":
                        "application/json"
                },

                body:
                    JSON.stringify(
                        payload
                    )
            }
        );

    const responseText =
        await response.text();

    console.log(
        "FCM HTTP:",
        response.status
    );

    return {

        success:
            response.ok,

        status:
            response.status,

        response:
            responseText
    };
}

/*
|--------------------------------------------------------------------------
| MAIN FUNCTION
|--------------------------------------------------------------------------
*/

Deno.serve(
    async (req: Request): Promise<Response> => {

        console.log(
            "================================================"
        );

        console.log(
            "STEP 1: bright-worker started"
        );

        try {

            /*
            |--------------------------------------------------------------------------
            | METHOD
            |--------------------------------------------------------------------------
            */

            console.log(
                "HTTP Method:",
                req.method
            );

            if (
                req.method !== "POST"
            ) {

                return jsonResponse(
                    {
                        success:
                            false,

                        message:
                            "POST only"
                    },
                    405
                );
            }

            /*
            |--------------------------------------------------------------------------
            | SECRET
            |--------------------------------------------------------------------------
            */

            console.log(
                "STEP 1A: Checking notification secret"
            );

            if (!NOTIFICATION_SECRET) {

                throw new Error(
                    "NOTIFICATION_SECRET secret is missing"
                );
            }

            const requestSecret =
                req.headers.get(
                    "x-notification-secret"
                );

            if (
                !requestSecret ||
                requestSecret !==
                    NOTIFICATION_SECRET
            ) {

                console.error(
                    "ERROR: Invalid notification secret"
                );

                return jsonResponse(
                    {
                        success:
                            false,

                        message:
                            "Unauthorized"
                    },
                    401
                );
            }

            console.log(
                "STEP 1B: Notification secret OK"
            );

            /*
            |--------------------------------------------------------------------------
            | SUPABASE SECRET KEY
            |--------------------------------------------------------------------------
            */

            const supabaseSecretKey =
                getSupabaseSecretKey();

            console.log(
                "STEP 1C: Supabase secret key loaded"
            );

            /*
            |--------------------------------------------------------------------------
            | REQUEST JSON
            |--------------------------------------------------------------------------
            */

            let requestBody: unknown;

            try {

                requestBody =
                    await req.json();

            } catch {

                return jsonResponse(
                    {
                        success:
                            false,

                        message:
                            "Invalid JSON"
                    },
                    400
                );
            }

            console.log(
                "STEP 2: Request body received"
            );

            const order =
                (
                    requestBody &&
                    typeof requestBody === "object" &&
                    "order" in requestBody &&
                    (requestBody as Record<string, unknown>).order
                )
                    ? (requestBody as Record<string, unknown>).order
                    : requestBody;

            if (
                !order ||
                typeof order !== "object"
            ) {

                return jsonResponse(
                    {
                        success:
                            false,

                        message:
                            "Invalid order data"
                    },
                    400
                );
            }

            const orderData =
                order as Record<string, unknown>;

            /*
            |--------------------------------------------------------------------------
            | VALIDATE ORDER
            |--------------------------------------------------------------------------
            */

            if (
                !orderData.order_id &&
                !orderData.order_number
            ) {

                return jsonResponse(
                    {
                        success:
                            false,

                        message:
                            "order_id or order_number is required"
                    },
                    400
                );
            }

            console.log(
                "Order ID:",
                orderData.order_id ?? "none"
            );

            console.log(
                "Order Number:",
                orderData.order_number ?? "none"
            );

            /*
            |--------------------------------------------------------------------------
            | TOKENS
            |--------------------------------------------------------------------------
            */

            const tokens =
                await getActiveTokens(
                    supabaseSecretKey
                );

            if (
                tokens.length === 0
            ) {

                return jsonResponse({

                    success:
                        false,

                    message:
                        "No active FCM tokens found",

                    sent:
                        0,

                    failed:
                        0
                });
            }

            /*
            |--------------------------------------------------------------------------
            | FIREBASE
            |--------------------------------------------------------------------------
            */

            const firebase =
                await getFirebaseAccessToken();

            console.log(
                "STEP 5: Firebase project:",
                firebase.projectId
            );

            /*
            |--------------------------------------------------------------------------
            | SEND
            |--------------------------------------------------------------------------
            */

            let sent = 0;

            let failed = 0;

            const results = [];

            for (
                const row of tokens
            ) {

                console.log(
                    "Sending notification to token ID:",
                    row.id
                );

                try {

                    const result =
                        await sendFCM(
                            firebase.accessToken,
                            firebase.projectId,
                            String(row.token),
                            orderData
                        );

                    if (
                        result.success
                    ) {

                        sent++;

                    } else {

                        failed++;

                        /*
                        |----------------------------------------------------------------------
                        | FCM 400/404
                        |----------------------------------------------------------------------
                        */

                        if (
                            result.status === 400 ||
                            result.status === 404
                        ) {

                            await disableToken(
                                supabaseSecretKey,
                                row.id
                            );
                        }
                    }

                    results.push({

                        id:
                            row.id,

                        success:
                            result.success,

                        status:
                            result.status,

                        response:
                            result.response
                    });

                } catch (error) {

                    failed++;

                    console.error(
                        "FCM token ERROR:",
                        row.id,
                        error instanceof Error
                            ? error.message
                            : String(error)
                    );

                    results.push({

                        id:
                            row.id,

                        success:
                            false,

                        error:
                            error instanceof Error
                                ? error.message
                                : String(error)
                    });
                }
            }

            /*
            |--------------------------------------------------------------------------
            | FINAL RESPONSE
            |--------------------------------------------------------------------------
            */

            console.log(
                "STEP 7: Notification process completed"
            );

            console.log(
                "Sent:",
                sent
            );

            console.log(
                "Failed:",
                failed
            );

            return jsonResponse({

                success:
                    sent > 0,

                message:
                    sent > 0
                        ? "Notification sent successfully"
                        : "Notification failed",

                order_id:
                    orderData.order_id ?? null,

                order_number:
                    orderData.order_number ?? null,

                token_count:
                    tokens.length,

                sent:
                    sent,

                failed:
                    failed,

                results:
                    results
            });

        } catch (error) {

            console.error(
                "================================================"
            );

            console.error(
                "FATAL FUNCTION ERROR:",
                error instanceof Error
                    ? error.message
                    : String(error)
            );

            console.error(
                "================================================"
            );

            return jsonResponse(
                {
                    success:
                        false,

                    message:
                        error instanceof Error
                            ? error.message
                            : String(error)
                },
                500
            );
        }
    }
);
