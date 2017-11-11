package com.example.blurryface.customprint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.FileOutputStream;
import java.io.IOException;

public class CustomPrintActivity extends AppCompatActivity {
    //create a custom print Adapter
    public class MyPrintDocumentAdapter extends PrintDocumentAdapter{
        Context context;
        private int PageHeight,PageWidth;
        public PdfDocument myPdfDocument;
        private int totalPages = 4;
        public MyPrintDocumentAdapter(Context context){
            this.context = context;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle) {
           //initialize the attributes
            myPdfDocument = new PrintedPdfDocument(context,newAttributes);
            PageHeight = newAttributes.getMediaSize().getHeightMils()/1000*72;
            PageWidth = newAttributes.getMediaSize().getWidthMils()/1000*72;
            //check if there is a cancellation event
            if(cancellationSignal.isCanceled()){
                layoutResultCallback.onLayoutCancelled();
                return;
            }
            if(totalPages>0){
                //document info eg name,type and page count
                PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder("print_output.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(totalPages);
                PrintDocumentInfo info = builder.build();
                layoutResultCallback.onLayoutFinished(info,true);
            }
            else
                layoutResultCallback.onLayoutFailed("Page count is zero");



        }

        @Override
        public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
            //ensuring that each page specified by user is printed
            for (int i=0;i<totalPages;i++){
                if(pagesInRange(pageRanges,i)) {
                    //create a new page for each page
                    PdfDocument.PageInfo newPage = new PdfDocument.PageInfo.Builder(PageWidth, PageHeight, i).create();
                    PdfDocument.Page page = myPdfDocument.startPage(newPage);

                    //check if cancelled
                    if (cancellationSignal.isCanceled()) {
                        writeResultCallback.onWriteCancelled();
                        myPdfDocument.close();
                        myPdfDocument = null;
                        return;
                    }
                    //drawing content onto the currentPage
                    drawPage(page,i);
                    myPdfDocument.finishPage(page);
                }
            }
            try {
                myPdfDocument.writeTo(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
            }catch (IOException e){
                writeResultCallback.onWriteFailed(e.toString());
                return;
            }finally {
                myPdfDocument.close();
                myPdfDocument=null;
            }
            writeResultCallback.onWriteFinished(pageRanges);
        }
        //method that checks whether the page specified is in range
        public boolean pagesInRange(PageRange[] pageRanges,int page){
            for (int i=0;i<pageRanges.length;i++){
                //if page is in the range return true
                if((page>=pageRanges[i].getStart())&&(page<=pageRanges[i].getEnd()))
                    return true;
            }
            return false;
        }
        //drawing content on the page canvas
        private void drawPage(PdfDocument.Page page, int pageNo){
            //getting the page canvas
            Canvas canvas = page.getCanvas();
            //make sure that the page Number starts from page Number one
            pageNo++;
            int titleBaseLine = 72,leftMargin=54;
            //choosing the background Colour of the canvas
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            //set the text size if the title
            paint.setTextSize(40);
            //title text
            canvas.drawText("Test Print Document Page "+ pageNo,leftMargin,titleBaseLine,paint);
            //reduce text size for the body
            paint.setTextSize(14);

            canvas.drawText("This is some test content to verify that custom printing works",leftMargin,titleBaseLine+35,paint);
            //draw the the even numbered pages red and odd green
            if(pageNo%2==0)
                paint.setColor(Color.RED);
            else
                paint.setColor(Color.GREEN);
            PdfDocument.PageInfo pageInfo = page.getInfo();
            canvas.drawCircle(pageInfo.getPageWidth()/2,pageInfo.getPageHeight()/2,150,paint);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_print);
    }
    protected void printDocument(View view){
        PrintManager printManager = (PrintManager) this.getSystemService(PRINT_SERVICE);
        String jobName = getString(R.string.app_name)+"PrintTest";
        printManager.print(jobName, new MyPrintDocumentAdapter(this),null);
    }
}
