import { applyDecorators } from '@nestjs/common';
import { ApiOperation, ApiResponse } from '@nestjs/swagger';

const ERROR_NAMES: Record<number, string> = {
  400: 'Bad Request',
  401: 'Unauthorized',
  402: 'Payment Required',
  403: 'Forbidden',
  404: 'Not Found',
  409: 'Conflict',
  429: 'Too Many Requests',
  502: 'Bad Gateway',
};

// Services return plain object literals (no response classes), so Swagger can't infer
// anything - each route documents itself with a real example body instead.
export function ApiDoc(
  summary: string,
  example: unknown,
  opts: { status?: number; description?: string; errors?: Record<number, string> } = {},
) {
  return applyDecorators(
    ApiOperation({ summary, description: opts.description }),
    ApiResponse({ status: opts.status ?? 200, description: 'Success', schema: { example } }),
    // Error bodies use Nest's default HttpException shape.
    ...Object.entries(opts.errors ?? {}).map(([code, message]) =>
      ApiResponse({
        status: Number(code),
        description: message,
        schema: { example: { statusCode: Number(code), message, error: ERROR_NAMES[Number(code)] } },
      }),
    ),
  );
}

export const msg = (message: string) => ({ message });

// Jamendo payloads are passed through untouched - these mirror its v3.0 /tracks and /artists items.
export const JAMENDO_TRACK = {
  id: '1204669',
  name: 'Wake Up',
  duration: 204,
  artist_id: '7872',
  artist_name: 'Jekk',
  album_name: 'Dreams',
  album_id: '145270',
  position: 1,
  releasedate: '2015-03-02',
  album_image: 'https://usercontent.jamendo.com?type=album&id=145270&width=300',
  audio: 'https://prod-1.storage.jamendo.com/?trackid=1204669&format=mp31',
  audiodownload: 'https://prod-1.storage.jamendo.com/download/track/1204669/mp32/',
  image: 'https://usercontent.jamendo.com?type=album&id=145270&width=300',
  shareurl: 'https://www.jamendo.com/track/1204669',
  audiodownload_allowed: true,
};

export const JAMENDO_ARTIST = {
  id: '7872',
  name: 'Jekk',
  website: 'https://jekk.example',
  joindate: '2008-11-20',
  image: 'https://usercontent.jamendo.com?type=artist&id=7872&width=300',
  shareurl: 'https://www.jamendo.com/artist/7872',
};

export const jamendoList = (item: unknown) => ({
  headers: { status: 'success', code: 0, error_message: '', warnings: '', results_count: 1 },
  results: [item],
});
