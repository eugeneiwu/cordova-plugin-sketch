#import "BGSketchPlugin.h"
#import "BGTouchDrawViewController.h"
#import "UIImage+ImageWithColor.h"

enum DestinationType {
    DestinationTypeDataUrl = 0,
    DestinationTypeFileUri
};
typedef NSUInteger DestinationType;

enum EncodingType {
    EncodingTypeJPEG = 0,
    EncodingTypePNG
};
typedef NSUInteger EncodingType;

enum InputType {
    InputTypeNoInput = 0,
    InputTypeDataUrl,
    InputTypeFileUri
};
typedef NSUInteger InputType;

@interface BGSketchPlugin () <SaveDrawingProtocol>

@property (copy) NSString* callbackId;
@property (nonatomic) DestinationType destinationType;
@property (nonatomic) EncodingType encodingType;
@property (nonatomic) InputType inputType;

@property (nonatomic) BOOL showStrokeWidthSelect;
@property (nonatomic) BOOL showColorSelect;
@property (nonatomic) NSUInteger setStrokeWidth;

@property (nonatomic) NSString *toolbarBackgroundColor;
@property (nonatomic) NSString *toolbarTextColor;

@property (nonatomic, copy) NSString *inputData;
@property (nonatomic, retain) UINavigationController *navigationController;
@property (nonatomic, retain) BGTouchDrawViewController *touchDrawController;

@end

@implementation BGSketchPlugin

- (void) getSketch:(CDVInvokedUrlCommand*)command
{
    self.callbackId = command.callbackId;
    NSDictionary *options = command.arguments[0];

    if (!options || options.count == 0) {
        [self sendErrorResultWithMessage:@"Insufficent number of options"];
        return;
    }
    
    if ([options objectForKey:@"toolbarBgColor"]) {
        if([options[@"toolbarBgColor"] length] == 7) {
            NSLog(@"set toolbarBackgroundColor = %@", options[@"toolbarBgColor"]);
            self.toolbarBackgroundColor = [options[@"toolbarBgColor"] stringValue];
        } else {
            NSLog(@"option toolbarBgColor wrong - to default true - given = %@", options[@"toolbarBgColor"]);
            self.toolbarBackgroundColor = @"#000000";
        }
    } else {
        NSLog(@"option toolbarBgColor not set - to default true");
        self.toolbarBackgroundColor = @"#000000";
    }
    
    if ([options objectForKey:@"toolbarTextColor"]) {
        if([options[@"toolbarTextColor"] length] == 7) {
            NSLog(@"set toolbarTextColor = %@", options[@"toolbarTextColor"]);
            self.toolbarTextColor = [options[@"toolbarTextColor"] stringValue];
        } else {
            NSLog(@"option toolbarTextColor wrong - to default true - given = %@", options[@"toolbarTextColor"]);
            self.toolbarTextColor = @"#FFFFFF";
        }
    } else {
        NSLog(@"option toolbarTextColor not set - to default true");
        self.toolbarTextColor = @"#FFFFFF";
    }
    
    
    if ([options objectForKey:@"showStrokeWidthSelect"]) {
        NSLog(@"set showStrokeWidthSelect = %@", options[@"showStrokeWidthSelect"]);
        self.showStrokeWidthSelect = [options[@"showStrokeWidthSelect"] boolValue];
    } else {
        NSLog(@"option showStrokeWidthSelect not set - to default true");
        self.showStrokeWidthSelect = true;
    }
    
    if ([options objectForKey:@"showColorSelect"]) {
        NSLog(@"set showColorSelect = %@", options[@"showColorSelect"]);
        self.showColorSelect = [options[@"showColorSelect"] boolValue];
    } else {
        NSLog(@"option showColorSelect not set - to default true");
        self.showColorSelect = true;
    }
    
    if ([options objectForKey:@"setStrokeWidth"]) {
        NSLog(@"set setStrokeWidth = %@", options[@"setStrokeWidth"]);
        self.setStrokeWidth = [options[@"setStrokeWidth"] integerValue];
    } else {
        NSLog(@"option setStrokeWidth not set - to default 2");
        self.setStrokeWidth = 2;
    }

    NSUInteger destinationType = [options[@"destinationType"] integerValue];
    switch (destinationType) {
        case DestinationTypeDataUrl:
        case DestinationTypeFileUri:
            self.destinationType = destinationType;
            break;
        default:
            [self sendErrorResultWithMessage:@"Invalid destinationType"];
            return;

    }

    NSUInteger encodingType = [options[@"encodingType"] integerValue];
    switch (encodingType) {
        case EncodingTypeJPEG:
        case EncodingTypePNG:
            self.encodingType = encodingType;
            break;
        default:
            [self sendErrorResultWithMessage:@"Invalid encodingType"];
            return;
    }

    NSUInteger inputType = [options[@"inputType"] integerValue];
    switch (inputType) {
        case InputTypeNoInput:
        case InputTypeDataUrl:
        case InputTypeFileUri:
            self.inputType = inputType;
            break;
        default:
            [self sendErrorResultWithMessage:@"Invalid inputType"];
            return;
    }

    if (self.inputType != InputTypeNoInput) {
        NSString *inputData = options[@"inputData"];

        if (!inputData || [inputData isEqualToString:@""]) {
            [self sendErrorResultWithMessage:@"inputData not given"];
            return;
        }

        self.inputData = inputData;
    } else {
        self.inputData = nil;
    }

    if (self.inputData) {
        [self doAnnotation];
    } else {
        [self doSketch];
    }
}

- (void) doAnnotation
{
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        UIImage *backgroundImage = nil;
        NSData *imageData = nil;

        if (self.inputType == InputTypeDataUrl) {
            imageData = [NSData dataWithContentsOfURL:[NSURL URLWithString:self.inputData]];

            if (!imageData) {
                [self sendErrorResultWithMessage:@"Failed to read image data from data url"];
                return;
            }

            self.inputData = nil; // release the base64 image data
        } else if (self.inputType == InputTypeFileUri) {
            imageData = [NSData dataWithContentsOfFile:[[NSURL URLWithString:self.inputData] relativePath]];

            if (!imageData) {
                [self sendErrorResultWithMessage:@"Failed to read image data from file"];
                return;
            }
        }

        backgroundImage = [UIImage imageWithData:imageData];
        if (!backgroundImage) {
            [self sendErrorResultWithMessage:@"Failed to created background image from input data"];
            return;
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            CGSize contentSize = CGSizeMake(backgroundImage.size.width, backgroundImage.size.height);
            BGTouchDrawViewController *touchDrawVC = [[BGTouchDrawViewController alloc] init];
            touchDrawVC.backgroundImage = backgroundImage;
            touchDrawVC.contentSize = contentSize;
            touchDrawVC.delegate = self;
            touchDrawVC.shouldCenterDrawing = YES;
            touchDrawVC.shouldResizeContentOnRotate = NO;
            
            touchDrawVC.showStrokeWidthSelect = self.showStrokeWidthSelect;
            touchDrawVC.showColorSelect = self.showColorSelect;
            touchDrawVC.setStrokeWidth = self.setStrokeWidth;
            
            touchDrawVC.toolbarBackgroundColor = self.toolbarBackgroundColor;
            touchDrawVC.toolbarTextColor = self.toolbarTextColor;
            
            self.touchDrawController = touchDrawVC;

            UIViewController *rootVC = [[UIViewController alloc] init]; // dummy root view controller
            UINavigationController *navVC = [[UINavigationController alloc] initWithRootViewController:rootVC];
            [rootVC.navigationController setNavigationBarHidden:YES];
            [navVC pushViewController:touchDrawVC animated:NO];
            
            // ios 13 fix
            [navVC setModalPresentationStyle:UIModalPresentationFullScreen];
            
            [self.viewController presentViewController:navVC animated:YES completion:nil];
            self.navigationController = navVC;
        });
    });
}

- (void) doSketch
{
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        CGSize contentSize = self.viewController.view.bounds.size;
        UIImage *backgroundImage = [UIImage imageWithColor:[UIColor whiteColor]
                                                   andSize:CGRectMake(0.0f, 0.0f, contentSize.width, contentSize.height)];

        dispatch_async(dispatch_get_main_queue(), ^{
            BGTouchDrawViewController *touchDrawVC = [[BGTouchDrawViewController alloc] init];
            touchDrawVC.backgroundImage = backgroundImage;
            touchDrawVC.contentSize = contentSize;
            touchDrawVC.delegate = self;
            touchDrawVC.shouldCenterDrawing = NO;
            touchDrawVC.shouldResizeContentOnRotate = YES;
            
            touchDrawVC.showStrokeWidthSelect = self.showStrokeWidthSelect;
            touchDrawVC.showColorSelect = self.showColorSelect;
            touchDrawVC.setStrokeWidth = self.setStrokeWidth;
            
            touchDrawVC.toolbarBackgroundColor = self.toolbarBackgroundColor;
            touchDrawVC.toolbarTextColor = self.toolbarTextColor;
            
            self.touchDrawController = touchDrawVC;

            UIViewController *rootVC = [[UIViewController alloc] init]; // dummy root view controller.
            UINavigationController *navVC = [[UINavigationController alloc] initWithRootViewController:rootVC];
            [rootVC.navigationController setNavigationBarHidden:YES];
            [navVC pushViewController:touchDrawVC animated:NO];
            
            // ios 13 fix
            [navVC setModalPresentationStyle:UIModalPresentationFullScreen];
            
            [self.viewController presentViewController:navVC animated:YES completion:nil];
            self.navigationController = navVC;
        });
    });
}

- (void) sendErrorResultWithMessage:(NSString *)message
{
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                             messageAsString:message]
                                callbackId:self.callbackId];
}

- (void) dealloc
{
    if (self.touchDrawController) {
        self.touchDrawController.delegate = nil;
        self.touchDrawController = nil;
    }

    if (self.navigationController) {
        self.navigationController = nil;
    }
}

#pragma mark - SaveDrawingProtocol

- (void) saveDrawing:(UIImage *)drawing
{
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSData *drawingData;
        NSString *encType;
        if (self.encodingType == EncodingTypeJPEG) {
            drawingData = UIImageJPEGRepresentation(drawing, 1.0f);
            encType = @"jpeg";
        } else if (self.encodingType == EncodingTypePNG){
            drawingData = UIImagePNGRepresentation(drawing);
            encType = @"png";
        }

        NSString *message = nil;
        if (self.destinationType == DestinationTypeDataUrl) {
            message = [NSString stringWithFormat:@"data:image/%@;base64,%@", encType, [drawingData base64EncodedStringWithOptions:0]];
        } else if (self.destinationType == DestinationTypeFileUri) {
            NSString *fileName = [NSString stringWithFormat:@"sketch-%@.%@", [NSUUID UUID].UUIDString, encType];
            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSURL *filePath = [NSURL fileURLWithPath:[paths[0] stringByAppendingPathComponent:fileName]];
            NSError *error = nil;

            [drawingData writeToURL:filePath options:(NSDataWritingAtomic|NSDataWritingFileProtectionComplete)
                              error:&error];
            if (error) {
                [self sendErrorResultWithMessage:[@"Failed to write drawing data to file: " stringByAppendingString:[error localizedDescription]]];
                NSLog(@"Error: Failed to write drawing data to temp file: %@", [error localizedDescription]);
            } else {
                message = [filePath relativePath];
            }
        }

        if (message) {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                                     messageAsString:message]
                                        callbackId:self.callbackId];
            NSLog(@"Drawing saved");
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            [self.navigationController popToRootViewControllerAnimated:NO];
            [self.navigationController dismissViewControllerAnimated:NO completion:nil];
        });
    });
}

- (void) cancelDrawing
{
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                             messageAsString:@""]
                                callbackId:self.callbackId];

    [self.navigationController popToRootViewControllerAnimated:NO];
    [self.navigationController dismissViewControllerAnimated:NO completion:nil];
    NSLog(@"Drawing cancelled");
}

@end
